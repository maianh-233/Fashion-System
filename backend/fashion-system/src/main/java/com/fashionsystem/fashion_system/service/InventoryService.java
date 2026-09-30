package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.*;
import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.*;
import com.fashionsystem.fashion_system.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("INVENTORY")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryService {
    private static final Set<String> BALANCE_SORT = Set.of("storeId","productVariantId","availableQuantity","onlineQuantity","updatedAt");
    private static final Set<String> MOVEMENT_SORT = Set.of("id","createdAt","transactionType","quantity","balanceAfter");
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final StoreRepository storeRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final InventoryBalanceMapper balanceMapper;
    private final InventoryTransactionMapper transactionMapper;
    private final StoreAccessService storeAccess;
    private final UserScopeService userScopeService;

    public InventoryBalanceDto getBalance(UUID actor, UUID store, UUID variant) {
        store = storeAccess.requireAny(actor,store,List.of("INVENTORY_VIEW","EXPORT_RECEIPT_VIEW","EXPORT_RECEIPT_CREATE","EXPORT_RECEIPT_UPDATE"));
        requireReferences(store,variant);
        InventoryBalance b = balanceRepository.findById(new InventoryBalanceId(store,variant)).orElse(null);
        if(b == null) b = InventoryBalance.builder().storeId(store).productVariantId(variant)
            .availableQuantity(0).onlineQuantity(0).reservedQuantity(0).damagedQuantity(0).build();
        return describe(b);
    }
    public Page<InventoryBalanceDto> getBalances(UUID actor,UUID store,UUID variant,Integer threshold,Pageable page) {
        return getBalances(actor,store,variant,threshold,"",null,"ALL",page);
    }
    public Page<InventoryBalanceDto> getBalances(UUID actor,UUID store,UUID variant,Integer threshold,String keyword,UUID category,String status,Pageable page) {
        store = storeAccess.require(actor,store,"INVENTORY_VIEW");
        validatePage(page,BALANCE_SORT);
        if(threshold != null && threshold < 0) throw BusinessException.badRequest("Invalid stock threshold");
        String stock = status == null ? "ALL" : status.toUpperCase(Locale.ROOT);
        if(!Set.of("ALL","LOW","OUT","IN").contains(stock)) throw BusinessException.badRequest("Invalid stock filter");
        return balanceRepository.searchWarehouse(store,variant,threshold,keyword == null ? "" : keyword.trim(),category,stock,page).map(this::describe);
    }
    public Page<InventoryTransactionDto> getTransactions(UUID actor,UUID store,UUID variant,String type,String referenceType,UUID reference,Pageable page) {
        store = storeAccess.require(actor,store,"INVENTORY_VIEW");
        validatePage(page,MOVEMENT_SORT);
        return transactionRepository.search(store,variant,normalize(type),normalize(referenceType),reference,page).map(transactionMapper::toDto);
    }
    public InventoryStatisticsDto getStatistics(UUID actor,UUID store,int threshold) {
        store = storeAccess.require(actor,store,"INVENTORY_VIEW");
        if(threshold<0) throw BusinessException.badRequest("Invalid stock threshold");
        var total = balanceRepository.summarize(store,threshold);
        var scope = userScopeService.resolve(actor);
        return new InventoryStatisticsDto(scope.kind().name(),store,total.storeName(),total,List.of(total));
    }
    @Transactional
    public InventoryBalanceDto adjust(UUID actor,UUID store,UUID variant,int delta) {
        store = storeAccess.require(actor,store,"INVENTORY_ADJUST");
        if(delta==0) throw BusinessException.badRequest("Quantity must be nonzero");
        var b=lockBalance(store,variant);
        return mutate(b,delta,0,"ADJUST","MANUAL_ADJUSTMENT",null,actor,delta<0?"OFFLINE":null,delta>0?"OFFLINE":null);
    }
    @Transactional
    public InventoryBalanceDto receiveChannel(UUID store,UUID variant,int qty,String channel,UUID receipt,UUID actor) {
        storeAccess.require(actor,store,"IMPORT_RECEIPT_COMPLETE");
        positive(qty); channel(channel);
        var b=lockBalance(store,variant);
        return mutate(b,"OFFLINE".equals(channel)?qty:0,"ONLINE".equals(channel)?qty:0,"IMPORT","GOODS_RECEIPT",receipt,actor,null,channel);
    }
    @Transactional
    public InventoryBalanceDto exportChannel(UUID store,UUID variant,int qty,String channel,String type,UUID receipt,UUID actor) {
        storeAccess.require(actor,store,"EXPORT_RECEIPT_COMPLETE");
        positive(qty); channel(channel);
        if(!Set.of("ONLINE_TO_OFFLINE","OFFLINE_TO_ONLINE","DAMAGED","RETURN_TO_SUPPLIER","OTHER").contains(type))
            throw BusinessException.badRequest("Invalid export type");
        if(("ONLINE_TO_OFFLINE".equals(type)&&!"ONLINE".equals(channel)) || ("OFFLINE_TO_ONLINE".equals(type)&&!"OFFLINE".equals(channel)))
            throw BusinessException.badRequest("Transfer source does not match type");
        var b=lockBalance(store,variant);
        int offline="OFFLINE".equals(channel)?-qty:0, online="ONLINE".equals(channel)?-qty:0;
        String target=null;
        if("ONLINE_TO_OFFLINE".equals(type)) {offline=qty; target="OFFLINE";}
        if("OFFLINE_TO_ONLINE".equals(type)) {online=qty; target="ONLINE";}
        String movement = switch(type) {case "DAMAGED" -> "EXPORT_DAMAGED"; case "OTHER" -> "OTHER_EXPORT"; default -> type;};
        return mutate(b,offline,online,movement,"GOODS_ISSUE",receipt,actor,channel,target);
    }
    /** Legacy internal entry points retain the same security and transactional rules. */
    @Transactional public InventoryBalanceDto receive(UUID store,UUID variant,int qty,UUID ref,UUID actor) {
        return receiveChannel(store,variant,qty,"OFFLINE",ref,actor);
    }
    @Transactional public InventoryBalanceDto issue(UUID store,UUID variant,int qty,UUID ref,UUID actor) {
        return exportChannel(store,variant,qty,"OFFLINE","OTHER",ref,actor);
    }
    /** Existing reservations use the legacy OFFLINE bucket; no historical reservations are reclassified. */
    @Transactional public InventoryBalanceDto reserve(UUID store,UUID variant,int qty,UUID ref,UUID actor) {
        storeAccess.require(actor,store,"INVENTORY_ADJUST"); positive(qty);
        var b=lockBalance(store,variant);
        int reserved=checked((long)b.getReservedQuantity()+qty);
        if(b.getOfflineQuantity()<qty) throw BusinessException.invalidState("Insufficient offline stock");
        b.setReservedQuantity(reserved);
        return mutate(b,-qty,0,"RESERVE","STOCK_RESERVATION",ref,actor,"OFFLINE",null);
    }
    @Transactional public InventoryBalanceDto release(UUID store,UUID variant,int qty,UUID ref,UUID actor) {
        storeAccess.require(actor,store,"INVENTORY_ADJUST"); positive(qty);
        var b=lockBalance(store,variant);
        if(b.getReservedQuantity()<qty) throw BusinessException.invalidState("Insufficient reserved stock");
        b.setReservedQuantity(b.getReservedQuantity()-qty);
        return mutate(b,qty,0,"RELEASE","STOCK_RESERVATION",ref,actor,null,"OFFLINE");
    }
    @Transactional public InventoryBalanceDto consumeReservation(UUID store,UUID variant,int qty,UUID ref,UUID actor) {
        storeAccess.require(actor,store,"INVENTORY_ADJUST"); positive(qty);
        var b=lockBalance(store,variant);
        if(b.getReservedQuantity()<qty) throw BusinessException.invalidState("Insufficient reserved stock");
        b.setReservedQuantity(b.getReservedQuantity()-qty);
        var dto=mutate(b,0,0,"RESERVATION_CONSUMED","STOCK_RESERVATION",ref,actor,null,null,-qty);
        return dto;
    }
    private InventoryBalanceDto mutate(InventoryBalance b,int offlineDelta,int onlineDelta,String type,String referenceType,UUID reference,UUID actor,String from,String to) {
        return mutate(b,offlineDelta,onlineDelta,type,referenceType,reference,actor,from,to,null);
    }
    private InventoryBalanceDto mutate(InventoryBalance b,int offlineDelta,int onlineDelta,String type,String referenceType,UUID reference,UUID actor,String from,String to,Integer ledgerQuantity) {
        int oldOffline=b.getOfflineQuantity(),oldOnline=b.getOnlineQuantity();
        int offline=checked((long)oldOffline+offlineDelta),online=checked((long)oldOnline+onlineDelta);
        int total=checked((long)offline+online);
        b.setOfflineQuantity(offline); b.setOnlineQuantity(online); b.setUpdatedAt(LocalDateTime.now());
        balanceRepository.save(b);
        transactionRepository.save(InventoryTransaction.builder().storeId(b.getStoreId()).productVariantId(b.getProductVariantId())
            .transactionType(type).referenceType(referenceType).referenceId(reference)
            .importReceiptId("GOODS_RECEIPT".equals(referenceType)?reference:null)
            .exportReceiptId("GOODS_ISSUE".equals(referenceType)?reference:null)
            .quantity(ledgerQuantity!=null?ledgerQuantity:(from!=null&&to!=null?Math.abs(offlineDelta):offlineDelta+onlineDelta)).balanceAfter(total)
            .beforeOffline(oldOffline).afterOffline(offline).beforeOnline(oldOnline).afterOnline(online)
            .fromChannel(from).toChannel(to).createdBy(actor).createdAt(LocalDateTime.now()).build());
        return balanceMapper.toDto(b);
    }
    private InventoryBalance lockBalance(UUID store,UUID variant) {
        requireReferences(store,variant);
        balanceRepository.initialize(store,variant);
        return balanceRepository.findForUpdate(store,variant).orElseThrow(()->BusinessException.invalidState("Cannot initialize stock"));
    }
    private void requireReferences(UUID store,UUID variant) {
        if(store==null||!storeRepository.existsById(store)) throw BusinessException.notFound("Store not found");
        if(variant==null||!variantRepository.existsById(variant)) throw BusinessException.notFound("Variant not found");
    }
    private InventoryBalanceDto describe(InventoryBalance b) {
        var dto=balanceMapper.toDto(b);
        variantRepository.findById(b.getProductVariantId()).ifPresent(v->{
            dto.setProductId(v.getProductId()); dto.setSku(v.getSku());
            dto.setVariantName(Objects.toString(v.getColor(),"")+" / "+Objects.toString(v.getSize(),""));
            productRepository.findById(v.getProductId()).ifPresent(p->{dto.setProductName(p.getName());dto.setCategoryId(p.getCategoryId());});
        });
        return dto;
    }
    private int checked(long value) {
        if(value<0) throw BusinessException.invalidState("Insufficient stock / ton kho khong du");
        if(value>Integer.MAX_VALUE) throw BusinessException.badRequest("Stock quantity exceeds supported range");
        return (int)value;
    }
    private void positive(int qty) {if(qty<=0)throw BusinessException.badRequest("Quantity must be positive");}
    private void channel(String channel) {if(channel==null||!Set.of("ONLINE","OFFLINE").contains(channel))throw BusinessException.badRequest("Invalid stock channel");}
    private String normalize(String s) {return s==null?"":s.trim().toUpperCase(Locale.ROOT);}
    private void validatePage(Pageable page,Set<String> sort) {
        if(page.getPageSize()>100||page.getSort().stream().anyMatch(o->!sort.contains(o.getProperty()))) throw BusinessException.badRequest("Invalid pagination or sort");
    }
}
