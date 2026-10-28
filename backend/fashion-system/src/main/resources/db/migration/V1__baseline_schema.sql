-- Baseline schema of Lunaria Boutique, equivalent to old migrations V1..V41
-- (originals kept in db/archive for history). Generated with
-- pg_dump --schema-only from a database at V41 on 2026-10-28.
--
-- Databases that already had V41 applied by hand are marked as baselined by
-- Flyway (spring.flyway.baseline-version=2) and never run this file.

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    id uuid NOT NULL,
    action character varying(40) NOT NULL,
    actor_user_id uuid,
    changed_fields jsonb,
    created_at timestamp(6) without time zone NOT NULL,
    entity_id uuid,
    entity_type character varying(100),
    ip_address character varying(64),
    new_data jsonb,
    old_data jsonb,
    user_agent text,
    username character varying(50) NOT NULL,
    event_id uuid,
    actor_type character varying(16),
    changes jsonb,
    job_name character varying(128),
    method character varying(16),
    occurred_at timestamp(6) with time zone,
    path text,
    request_id character varying(128),
    row_count integer,
    CONSTRAINT audit_logs_actor_type_check CHECK (((actor_type)::text = ANY ((ARRAY['EMPLOYEE'::character varying, 'SYSTEM'::character varying])::text[])))
);

--
-- Name: audit_outbox; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_outbox (
    id uuid NOT NULL,
    attempt_count integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    event_id uuid NOT NULL,
    file_appended_at timestamp(6) with time zone,
    kafka_published_at timestamp(6) with time zone,
    last_error text,
    next_attempt_at timestamp(6) with time zone,
    occurred_at timestamp(6) with time zone NOT NULL,
    payload jsonb NOT NULL
);

--
-- Name: auth_audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auth_audit_logs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    action character varying(50) NOT NULL,
    description text,
    ip_address character varying(50),
    user_agent text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

--
-- Name: brands; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brands (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    code character varying(100),
    logo text,
    description text,
    status character varying(50) DEFAULT 'ACTIVE'::character varying,
    terminated_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    logo_public_id character varying(255)
);

--
-- Name: cart_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cart_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    customer_id uuid NOT NULL,
    product_variant_id uuid NOT NULL,
    quantity integer NOT NULL,
    selected boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT cart_items_quantity_check CHECK (((quantity >= 1) AND (quantity <= 99)))
);

--
-- Name: catalog_brand_code_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.catalog_brand_code_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: catalog_category_code_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.catalog_category_code_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: catalog_collection_code_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.catalog_collection_code_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: catalog_product_code_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.catalog_product_code_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: catalog_variant_sku_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.catalog_variant_sku_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.categories (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    parent_id uuid,
    name character varying(255) NOT NULL,
    code character varying(100),
    image_url text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    active boolean DEFAULT true NOT NULL
);

--
-- Name: chat_attachments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.chat_attachments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    message_id uuid NOT NULL,
    file_name character varying(255),
    file_url text NOT NULL,
    file_type character varying(100),
    file_size bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

--
-- Name: TABLE chat_attachments; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.chat_attachments IS 'Attachments linked to chat messages';

--
-- Name: collections; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.collections (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    brand_id uuid,
    name character varying(255) NOT NULL,
    code character varying(100),
    season character varying(50),
    year integer,
    release_date date,
    description text,
    image_url text,
    status character varying(50) DEFAULT 'ACTIVE'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    image_public_id character varying(255)
);

--
-- Name: customer_accounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_accounts (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    customer_id uuid NOT NULL,
    failed_login_attempts integer NOT NULL,
    login_email character varying(255),
    login_locked_until timestamp(6) without time zone,
    password_hash text,
    updated_at timestamp(6) without time zone NOT NULL,
    username character varying(50) NOT NULL
);

--
-- Name: customer_activity_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_activity_logs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    action character varying(50) NOT NULL,
    entity_type character varying(30) NOT NULL,
    entity_id uuid,
    metadata jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    customer_id uuid NOT NULL,
    CONSTRAINT customer_activity_logs_action_check CHECK (((action)::text = ANY ((ARRAY['UPDATE_PROFILE'::character varying, 'ADD_ADDRESS'::character varying, 'UPDATE_ADDRESS'::character varying, 'DELETE_ADDRESS'::character varying, 'SET_DEFAULT_ADDRESS'::character varying, 'CHANGE_TIER'::character varying, 'EARN_POINTS'::character varying, 'REDEEM_POINTS'::character varying])::text[]))),
    CONSTRAINT customer_activity_logs_entity_type_check CHECK (((entity_type)::text = ANY ((ARRAY['PROFILE'::character varying, 'ADDRESS'::character varying, 'TIER'::character varying, 'LOYALTY'::character varying])::text[])))
);

--
-- Name: customer_addresses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_addresses (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    receiver_name character varying(255) NOT NULL,
    receiver_phone character varying(20) NOT NULL,
    province character varying(100),
    district character varying(100),
    ward character varying(100),
    address_line text NOT NULL,
    postal_code character varying(20),
    latitude numeric(9,6),
    longitude numeric(9,6),
    is_default boolean DEFAULT false,
    address_type character varying(30) DEFAULT 'HOME'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    customer_id uuid NOT NULL,
    management_source character varying(20) NOT NULL,
    CONSTRAINT ck_customer_addresses_source CHECK (((management_source)::text = ANY ((ARRAY['STORE'::character varying, 'WEB'::character varying])::text[]))),
    CONSTRAINT customer_addresses_address_type_check CHECK (((address_type)::text = ANY ((ARRAY['HOME'::character varying, 'WORK'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT customer_addresses_latitude_check CHECK (((latitude >= ('-90'::integer)::numeric) AND (latitude <= (90)::numeric))),
    CONSTRAINT customer_addresses_longitude_check CHECK (((longitude >= ('-180'::integer)::numeric) AND (longitude <= (180)::numeric))),
    CONSTRAINT customer_addresses_management_source_check CHECK (((management_source)::text = ANY ((ARRAY['STORE'::character varying, 'WEB'::character varying])::text[])))
);

--
-- Name: customer_code_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.customer_code_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: customer_password_reset_challenges; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_password_reset_challenges (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    customer_account_id uuid NOT NULL,
    email character varying(255) NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    failed_attempts integer NOT NULL,
    otp_hash text NOT NULL,
    reset_token_expires_at timestamp(6) without time zone,
    reset_token_hash character varying(64),
    used_at timestamp(6) without time zone,
    verified_at timestamp(6) without time zone
);

--
-- Name: customer_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_profiles (
    user_id uuid NOT NULL,
    full_name character varying(255),
    date_of_birth date,
    gender character varying(20),
    avatar text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    CONSTRAINT customer_profiles_gender_check CHECK (((gender)::text = ANY ((ARRAY['MALE'::character varying, 'FEMALE'::character varying, 'OTHER'::character varying])::text[])))
);

--
-- Name: customer_promotions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_promotions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    customer_id uuid NOT NULL,
    promotion_id uuid NOT NULL,
    points_spent integer NOT NULL,
    redeemed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    used_order_id uuid,
    used_at timestamp without time zone,
    CONSTRAINT customer_promotions_points_spent_check CHECK ((points_spent > 0))
);

--
-- Name: customer_refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_refresh_tokens (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    customer_account_id uuid NOT NULL,
    device character varying(100),
    expires_at timestamp(6) without time zone NOT NULL,
    ip_address character varying(50),
    parent_token_id uuid,
    refresh_token_family uuid NOT NULL,
    revoked_at timestamp(6) without time zone,
    token_hash character varying(64) NOT NULL,
    user_agent text
);

--
-- Name: customer_social_accounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_social_accounts (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    customer_id uuid NOT NULL,
    last_login_at timestamp(6) without time zone,
    provider character varying(20) NOT NULL,
    provider_email character varying(255),
    provider_user_id character varying(255) NOT NULL
);

--
-- Name: customer_tier_assignments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_tier_assignments (
    id uuid NOT NULL,
    assigned_at timestamp(6) without time zone,
    customer_id uuid NOT NULL,
    expires_at timestamp(6) without time zone,
    note text,
    tier_id uuid NOT NULL
);

--
-- Name: customer_tiers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_tiers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    min_total_spent numeric(14,2) DEFAULT 0,
    discount_percent numeric(5,2) DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    CONSTRAINT customer_tiers_discount_percent_check CHECK (((discount_percent >= (0)::numeric) AND (discount_percent <= (100)::numeric))),
    CONSTRAINT customer_tiers_min_total_spent_check CHECK ((min_total_spent >= (0)::numeric))
);

--
-- Name: customers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customers (
    id uuid NOT NULL,
    active boolean DEFAULT true,
    avatar text,
    created_at timestamp(6) without time zone,
    date_of_birth date,
    email character varying(255),
    full_name character varying(255),
    gender character varying(20),
    phone character varying(20),
    updated_at timestamp(6) without time zone,
    customer_code character varying(30) NOT NULL,
    membership_status character varying(20) NOT NULL,
    normalized_phone character varying(20),
    origin_store_id uuid,
    source character varying(20) NOT NULL,
    note text,
    CONSTRAINT ck_customers_membership CHECK (((membership_status)::text = 'MEMBER'::text)),
    CONSTRAINT ck_customers_origin CHECK (((((source)::text = 'STORE'::text) AND (origin_store_id IS NOT NULL)) OR ((source)::text = 'WEBSITE'::text))),
    CONSTRAINT ck_customers_source CHECK (((source)::text = ANY ((ARRAY['STORE'::character varying, 'WEBSITE'::character varying])::text[]))),
    CONSTRAINT customers_membership_status_check CHECK (((membership_status)::text = 'MEMBER'::text)),
    CONSTRAINT customers_source_check CHECK (((source)::text = ANY ((ARRAY['STORE'::character varying, 'WEBSITE'::character varying])::text[])))
);

--
-- Name: departments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.departments (
    id uuid NOT NULL,
    active boolean,
    code character varying(50) NOT NULL,
    created_at timestamp(6) without time zone,
    description text,
    name character varying(150) NOT NULL,
    updated_at timestamp(6) without time zone
);

--
-- Name: goods_issue_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.goods_issue_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    issue_id uuid NOT NULL,
    product_variant_id uuid NOT NULL,
    sku character varying(100),
    product_name character varying(255),
    quantity integer NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    product_id uuid NOT NULL,
    source_channel character varying(20) NOT NULL,
    CONSTRAINT ck_warehouse_issue_item_channel CHECK (((source_channel)::text = ANY ((ARRAY['ONLINE'::character varying, 'OFFLINE'::character varying])::text[])))
);

--
-- Name: goods_issues; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.goods_issues (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    issue_code character varying(50) NOT NULL,
    store_id uuid NOT NULL,
    order_id uuid,
    issued_by uuid,
    approved_by uuid,
    issue_type character varying(50) NOT NULL,
    issue_date timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    status character varying(50) NOT NULL,
    note text,
    total_quantity integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    completed_at timestamp(6) without time zone,
    completed_by uuid,
    confirmed_at timestamp(6) without time zone,
    reason text,
    supplier_id uuid,
    CONSTRAINT ck_warehouse_issue_status CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PENDING_CONFIRMATION'::character varying, 'CONFIRMED'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT ck_warehouse_issue_type CHECK (((issue_type)::text = ANY ((ARRAY['RETURN_TO_SUPPLIER'::character varying, 'DAMAGED'::character varying, 'OTHER'::character varying, 'ONLINE_TO_OFFLINE'::character varying, 'OFFLINE_TO_ONLINE'::character varying])::text[])))
);

--
-- Name: goods_receipt_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.goods_receipt_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    receipt_id uuid NOT NULL,
    product_variant_id uuid NOT NULL,
    sku character varying(100),
    product_name character varying(255),
    cost_price numeric(12,2) NOT NULL,
    quantity integer NOT NULL,
    total numeric(14,2) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    product_id uuid NOT NULL,
    target_channel character varying(20) NOT NULL,
    CONSTRAINT ck_warehouse_receipt_item_channel CHECK (((target_channel)::text = ANY ((ARRAY['ONLINE'::character varying, 'OFFLINE'::character varying])::text[])))
);

--
-- Name: goods_receipts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.goods_receipts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    receipt_code character varying(50) NOT NULL,
    supplier_id uuid,
    store_id uuid NOT NULL,
    received_by uuid,
    approved_by uuid,
    receipt_date timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    status character varying(50) NOT NULL,
    note text,
    total_quantity integer DEFAULT 0,
    total_amount numeric(14,2) DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    completed_at timestamp(6) without time zone,
    completed_by uuid,
    confirmed_at timestamp(6) without time zone,
    CONSTRAINT ck_warehouse_receipt_status CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PENDING_CONFIRMATION'::character varying, 'CONFIRMED'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[])))
);

--
-- Name: internal_chat_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.internal_chat_messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    room_id uuid NOT NULL,
    sender_id uuid,
    sender_type character varying(20),
    content text,
    intent character varying(100),
    metadata jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

--
-- Name: TABLE internal_chat_messages; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.internal_chat_messages IS 'Messages inside internal chat rooms';

--
-- Name: internal_chat_rooms; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.internal_chat_rooms (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    created_by uuid,
    room_type character varying(20) DEFAULT 'PRIVATE'::character varying NOT NULL,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    closed_at timestamp without time zone
);

--
-- Name: TABLE internal_chat_rooms; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.internal_chat_rooms IS 'Internal communication rooms for staff/admin';

--
-- Name: inventory_balances; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inventory_balances (
    store_id uuid NOT NULL,
    product_variant_id uuid NOT NULL,
    available_quantity integer DEFAULT 0 NOT NULL,
    reserved_quantity integer DEFAULT 0 NOT NULL,
    damaged_quantity integer DEFAULT 0 NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    online_quantity integer NOT NULL,
    version bigint NOT NULL,
    CONSTRAINT ck_warehouse_balances_nonnegative CHECK (((available_quantity >= 0) AND (online_quantity >= 0) AND (reserved_quantity >= 0) AND (damaged_quantity >= 0) AND (version >= 0) AND (((available_quantity)::bigint + (online_quantity)::bigint) <= 2147483647)))
);

--
-- Name: COLUMN inventory_balances.available_quantity; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.inventory_balances.available_quantity IS 'Offline stock; retained legacy column name. Total stock = available_quantity + online_quantity.';

--
-- Name: inventory_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inventory_transactions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_variant_id uuid NOT NULL,
    store_id uuid NOT NULL,
    transaction_type character varying(50) NOT NULL,
    reference_type character varying(50),
    reference_id uuid,
    quantity integer NOT NULL,
    balance_after integer NOT NULL,
    created_by uuid,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    after_offline integer,
    after_online integer,
    before_offline integer,
    before_online integer,
    export_receipt_id uuid,
    from_channel character varying(255),
    import_receipt_id uuid,
    to_channel character varying(255),
    CONSTRAINT ck_warehouse_transaction_channels CHECK ((((from_channel IS NULL) OR ((from_channel)::text = ANY ((ARRAY['ONLINE'::character varying, 'OFFLINE'::character varying])::text[]))) AND ((to_channel IS NULL) OR ((to_channel)::text = ANY ((ARRAY['ONLINE'::character varying, 'OFFLINE'::character varying])::text[]))))),
    CONSTRAINT ck_warehouse_transaction_reference CHECK (((import_receipt_id IS NULL) OR (export_receipt_id IS NULL))),
    CONSTRAINT ck_warehouse_transaction_snapshots CHECK ((((before_offline IS NULL) AND (after_offline IS NULL) AND (before_online IS NULL) AND (after_online IS NULL)) OR ((before_offline IS NOT NULL) AND (after_offline IS NOT NULL) AND (before_online IS NOT NULL) AND (after_online IS NOT NULL) AND (before_offline >= 0) AND (after_offline >= 0) AND (before_online >= 0) AND (after_online >= 0) AND (((before_offline)::bigint + (before_online)::bigint) <= 2147483647) AND (((after_offline)::bigint + (after_online)::bigint) <= 2147483647))))
);

--
-- Name: loyalty_accounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loyalty_accounts (
    user_id uuid,
    total_spent numeric(14,2) DEFAULT 0,
    points_balance integer DEFAULT 0,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    customer_id uuid NOT NULL,
    CONSTRAINT loyalty_accounts_points_balance_check CHECK ((points_balance >= 0)),
    CONSTRAINT loyalty_accounts_total_spent_check CHECK ((total_spent >= (0)::numeric))
);

--
-- Name: loyalty_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loyalty_transactions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    transaction_type character varying(30) NOT NULL,
    points integer NOT NULL,
    reference_type character varying(50),
    reference_id uuid,
    note text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    customer_id uuid NOT NULL,
    amount numeric(14,2),
    CONSTRAINT loyalty_transactions_points_check CHECK (((points <> 0) OR (amount IS NOT NULL))),
    CONSTRAINT loyalty_transactions_transaction_type_check CHECK (((transaction_type)::text = ANY ((ARRAY['EARN'::character varying, 'REDEEM'::character varying, 'EXPIRE'::character varying, 'ADJUST'::character varying, 'REFUND_REVERSAL'::character varying])::text[])))
);

--
-- Name: modules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.modules (
    id uuid NOT NULL,
    active boolean NOT NULL,
    code character varying(50) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    description text,
    icon character varying(100),
    name character varying(100) NOT NULL,
    sort_order integer NOT NULL
);

--
-- Name: notification_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification_logs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    notification_id uuid NOT NULL,
    channel character varying(20) NOT NULL,
    destination character varying(255),
    status character varying(20) NOT NULL,
    error_message text,
    provider character varying(50),
    sent_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

--
-- Name: notification_templates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification_templates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(100) NOT NULL,
    title_template character varying(255),
    content_template text NOT NULL,
    channel character varying(20) NOT NULL,
    active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone
);

--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    recipient_id uuid,
    recipient_type character varying(20) NOT NULL,
    channel character varying(20) NOT NULL,
    title character varying(255),
    content text NOT NULL,
    reference_type character varying(50),
    reference_id uuid,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    scheduled_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    sent_at timestamp without time zone,
    event_id uuid NOT NULL,
    actor_user_id uuid,
    type character varying(80) NOT NULL,
    action_url character varying(500),
    metadata jsonb,
    read_at timestamp(6) without time zone
);

--
-- Name: order_addresses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_addresses (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    receiver_name character varying(255) NOT NULL,
    receiver_phone character varying(20) NOT NULL,
    province character varying(100),
    district character varying(100),
    ward character varying(100),
    address_line text,
    postal_code character varying(20),
    latitude numeric(9,6),
    longitude numeric(9,6),
    address_type character varying(20) DEFAULT 'SHIPPING'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT order_addresses_latitude_check CHECK (((latitude >= ('-90'::integer)::numeric) AND (latitude <= (90)::numeric))),
    CONSTRAINT order_addresses_longitude_check CHECK (((longitude >= ('-180'::integer)::numeric) AND (longitude <= (180)::numeric)))
);

--
-- Name: TABLE order_addresses; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.order_addresses IS 'Stores shipping address of an order';

--
-- Name: order_chat_message_status; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_chat_message_status (
    message_id uuid NOT NULL,
    user_id uuid NOT NULL,
    status character varying(20) DEFAULT 'SENT'::character varying NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

--
-- Name: TABLE order_chat_message_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.order_chat_message_status IS 'Message delivery/read status per user';

--
-- Name: order_chat_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_chat_messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    room_id uuid NOT NULL,
    sender_id uuid,
    sender_type character varying(20) NOT NULL,
    message_type character varying(20) DEFAULT 'TEXT'::character varying NOT NULL,
    content text,
    related_action character varying(50),
    metadata jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_order_chat_messages_sender CHECK (((sender_type)::text = ANY ((ARRAY['CUSTOMER'::character varying, 'BOT'::character varying, 'STAFF'::character varying, 'SYSTEM'::character varying])::text[])))
);

--
-- Name: TABLE order_chat_messages; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.order_chat_messages IS 'Messages exchanged in order chat rooms';

--
-- Name: order_chat_rooms; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_chat_rooms (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    customer_id uuid NOT NULL,
    assigned_staff_id uuid,
    status character varying(50) DEFAULT 'BOT_HANDLING'::character varying NOT NULL,
    last_message_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    closed_at timestamp without time zone,
    escalated_at timestamp without time zone,
    assigned_at timestamp without time zone,
    staff_unread_count integer DEFAULT 0 NOT NULL,
    customer_unread_count integer DEFAULT 0 NOT NULL,
    closed_by uuid,
    CONSTRAINT chk_order_chat_rooms_status CHECK (((status)::text = ANY ((ARRAY['BOT_HANDLING'::character varying, 'WAITING_STAFF'::character varying, 'STAFF_HANDLING'::character varying, 'CLOSED'::character varying])::text[])))
);

--
-- Name: TABLE order_chat_rooms; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.order_chat_rooms IS 'Chat rooms related to customer orders';

--
-- Name: order_code_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.order_code_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: order_customer_contacts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_customer_contacts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    customer_id uuid NOT NULL,
    sent_by uuid NOT NULL,
    subject character varying(200) NOT NULL,
    content text NOT NULL,
    via_email boolean DEFAULT false NOT NULL,
    via_notification boolean DEFAULT false NOT NULL,
    email_to character varying(255),
    email_status character varying(20) DEFAULT 'SKIPPED'::character varying NOT NULL,
    email_error text,
    notification_status character varying(20) DEFAULT 'SKIPPED'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_order_contacts_channel CHECK ((via_email OR via_notification)),
    CONSTRAINT chk_order_contacts_email_status CHECK (((email_status)::text = ANY ((ARRAY['SKIPPED'::character varying, 'PENDING'::character varying, 'SENT'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT chk_order_contacts_notification_status CHECK (((notification_status)::text = ANY ((ARRAY['SKIPPED'::character varying, 'PENDING'::character varying, 'SENT'::character varying, 'FAILED'::character varying])::text[])))
);

--
-- Name: order_issue_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_issue_types (
    code character varying(50) NOT NULL,
    name character varying(255) NOT NULL
);

--
-- Name: TABLE order_issue_types; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.order_issue_types IS 'Master data for order issue categories';

--
-- Name: order_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    product_id uuid,
    product_variant_id uuid NOT NULL,
    product_name character varying(255) NOT NULL,
    sku character varying(100),
    color character varying(100),
    size character varying(50),
    image_url text,
    price numeric(12,2) NOT NULL,
    quantity integer NOT NULL,
    total numeric(14,2) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    discount_amount numeric(14,2) DEFAULT 0 NOT NULL,
    promotion_id uuid,
    promotion_code character varying(50),
    CONSTRAINT chk_order_items_discount CHECK (((discount_amount >= (0)::numeric) AND (total >= (0)::numeric))),
    CONSTRAINT chk_order_items_price CHECK ((price >= (0)::numeric)),
    CONSTRAINT chk_order_items_quantity CHECK ((quantity > 0))
);

--
-- Name: TABLE order_items; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.order_items IS 'Stores items belonging to an order';

--
-- Name: order_promotions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_promotions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    promotion_id uuid NOT NULL,
    promotion_code character varying(50),
    discount_amount numeric(14,2) DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

--
-- Name: TABLE order_promotions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.order_promotions IS 'Stores promotions applied to orders';

--
-- Name: order_status_histories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_status_histories (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    from_status character varying(50),
    to_status character varying(50) NOT NULL,
    changed_by uuid,
    note text,
    changed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    actor_type character varying(20) DEFAULT 'USER'::character varying NOT NULL,
    reason text,
    CONSTRAINT chk_order_status_histories_actor CHECK (((actor_type)::text = ANY ((ARRAY['USER'::character varying, 'CUSTOMER'::character varying, 'SYSTEM'::character varying, 'SHIPPER'::character varying])::text[])))
);

--
-- Name: TABLE order_status_histories; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.order_status_histories IS 'Stores order status change history';

--
-- Name: orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orders (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_code character varying(50) NOT NULL,
    user_id uuid,
    store_id uuid,
    order_type character varying(20) NOT NULL,
    status character varying(50) NOT NULL,
    subtotal numeric(14,2) DEFAULT 0,
    discount_total numeric(14,2) DEFAULT 0,
    tax numeric(14,2) DEFAULT 0,
    shipping_fee numeric(14,2) DEFAULT 0,
    total_amount numeric(14,2) DEFAULT 0,
    payment_status character varying(50) DEFAULT 'UNPAID'::character varying,
    note text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    customer_id uuid,
    parent_order_id uuid,
    handler_user_id uuid,
    created_by uuid,
    payment_method character varying(30),
    cash_received numeric(14,2),
    change_amount numeric(14,2),
    cancel_reason text,
    cancelled_by_type character varying(20),
    active boolean DEFAULT true NOT NULL,
    hidden_by uuid,
    hidden_at timestamp without time zone,
    restored_by uuid,
    restored_at timestamp without time zone,
    client_request_id character varying(64),
    payment_due_at timestamp(6) without time zone,
    CONSTRAINT chk_orders_cancelled_by_type CHECK (((cancelled_by_type IS NULL) OR ((cancelled_by_type)::text = ANY ((ARRAY['USER'::character varying, 'CUSTOMER'::character varying, 'SYSTEM'::character varying, 'SHIPPER'::character varying])::text[])))),
    CONSTRAINT chk_orders_cash CHECK ((((cash_received IS NULL) OR (cash_received >= (0)::numeric)) AND ((change_amount IS NULL) OR (change_amount >= (0)::numeric)))),
    CONSTRAINT chk_orders_hidden_terminal CHECK ((active OR ((status)::text = ANY ((ARRAY['COMPLETED'::character varying, 'CANCELLED'::character varying, 'RETURNED'::character varying])::text[])))),
    CONSTRAINT chk_orders_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CONFIRMED'::character varying, 'HANDED_TO_SHIPPER'::character varying, 'SHIPPING'::character varying, 'COMPLETED'::character varying, 'DELIVERY_FAILED'::character varying, 'RETURNED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT chk_orders_total_amount CHECK ((total_amount >= (0)::numeric)),
    CONSTRAINT chk_orders_type CHECK (((order_type)::text = ANY ((ARRAY['ONLINE'::character varying, 'OFFLINE'::character varying])::text[])))
);

--
-- Name: TABLE orders; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.orders IS 'Stores customer orders';

--
-- Name: password_reset_otps; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.password_reset_otps (
    id uuid NOT NULL,
    account_id uuid NOT NULL,
    account_type character varying(20) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(255) NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    failed_attempts integer NOT NULL,
    otp_hash text NOT NULL,
    reset_token_expires_at timestamp(6) without time zone,
    reset_token_hash character varying(64),
    used_at timestamp(6) without time zone,
    verified_at timestamp(6) without time zone,
    CONSTRAINT password_reset_otps_account_type_check CHECK (((account_type)::text = ANY ((ARRAY['EMPLOYEE'::character varying, 'CUSTOMER'::character varying])::text[])))
);

--
-- Name: payment_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payment_transactions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    payment_id uuid NOT NULL,
    gateway_transaction_id character varying(100),
    transaction_type character varying(30) NOT NULL,
    amount numeric(14,2) NOT NULL,
    status character varying(50) NOT NULL,
    raw_response jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT payment_transactions_amount_check CHECK ((amount >= (0)::numeric))
);

--
-- Name: TABLE payment_transactions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.payment_transactions IS 'Store payment gateway transaction history';

--
-- Name: payment_webhook_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payment_webhook_logs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    provider character varying(50) NOT NULL,
    event_type character varying(100),
    payload jsonb NOT NULL,
    processed boolean DEFAULT false,
    processed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    event_key character varying(200),
    payment_id uuid,
    result character varying(50),
    signature_valid boolean
);

--
-- Name: TABLE payment_webhook_logs; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.payment_webhook_logs IS 'Store webhook logs received from payment providers';

--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    payment_code character varying(50),
    method character varying(50) NOT NULL,
    amount numeric(14,2) NOT NULL,
    status character varying(50) NOT NULL,
    transaction_code character varying(100),
    paid_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    expires_at timestamp(6) without time zone,
    failure_reason character varying(255),
    pay_url text,
    CONSTRAINT payments_amount_check CHECK ((amount >= (0)::numeric))
);

--
-- Name: TABLE payments; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.payments IS 'Store payment information for orders';

--
-- Name: permission_groups; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.permission_groups (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    code character varying(50) NOT NULL,
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    module_id uuid NOT NULL
);

--
-- Name: permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.permissions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(150) NOT NULL,
    code character varying(100) NOT NULL,
    group_id uuid,
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

--
-- Name: positions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.positions (
    id uuid NOT NULL,
    active boolean NOT NULL,
    code character varying(50) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    department_id uuid NOT NULL,
    description text,
    name character varying(150) NOT NULL,
    updated_at timestamp(6) without time zone,
    hierarchy_level integer DEFAULT 1 NOT NULL,
    min_salary bigint DEFAULT 0 NOT NULL,
    max_salary bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_positions_hierarchy_level CHECK ((hierarchy_level > 0)),
    CONSTRAINT ck_positions_min_salary CHECK ((min_salary >= 0)),
    CONSTRAINT ck_positions_salary_range CHECK ((max_salary >= min_salary))
);

--
-- Name: product_attributes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_attributes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_id uuid NOT NULL,
    attribute_name character varying(100) NOT NULL,
    attribute_value character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

--
-- Name: product_images; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_images (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_variant_id uuid NOT NULL,
    image_url text NOT NULL,
    is_primary boolean DEFAULT false,
    sort_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    cloudinary_public_id character varying(255)
);

--
-- Name: product_tag_mappings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_tag_mappings (
    product_id uuid NOT NULL,
    tag_id uuid NOT NULL
);

--
-- Name: product_tags; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_tags (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    active boolean DEFAULT true NOT NULL
);

--
-- Name: product_variants; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_variants (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_id uuid NOT NULL,
    sku character varying(100) NOT NULL,
    color character varying(100),
    size character varying(50),
    price numeric(12,2) NOT NULL,
    sale_price numeric(12,2),
    weight numeric(8,2),
    barcode character varying(100),
    active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone
);

--
-- Name: products; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.products (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    brand_id uuid,
    collection_id uuid,
    category_id uuid,
    name character varying(255) NOT NULL,
    slug character varying(255),
    description text,
    material character varying(255),
    fit character varying(100),
    gender character varying(20),
    status character varying(50) DEFAULT 'DRAFT'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    image_url text NOT NULL,
    code character varying(100),
    image_public_id character varying(255)
);

--
-- Name: promotion_conditions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.promotion_conditions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    promotion_id uuid NOT NULL,
    condition_type character varying(50) NOT NULL,
    condition_value character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

--
-- Name: TABLE promotion_conditions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.promotion_conditions IS 'Dynamic promotion conditions';

--
-- Name: promotion_products; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.promotion_products (
    promotion_id uuid NOT NULL,
    product_id uuid NOT NULL
);

--
-- Name: TABLE promotion_products; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.promotion_products IS 'Mapping promotion with products';

--
-- Name: promotion_tiers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.promotion_tiers (
    promotion_id uuid NOT NULL,
    tier_id uuid NOT NULL
);

--
-- Name: TABLE promotion_tiers; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.promotion_tiers IS 'Mapping promotion with membership tiers';

--
-- Name: promotion_usages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.promotion_usages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    promotion_id uuid NOT NULL,
    order_id uuid NOT NULL,
    user_id uuid,
    used_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    customer_id uuid
);

--
-- Name: TABLE promotion_usages; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.promotion_usages IS 'Promotion usage history';

--
-- Name: promotions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.promotions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    discount_type character varying(20) NOT NULL,
    discount_value numeric(10,2) NOT NULL,
    start_date timestamp without time zone NOT NULL,
    end_date timestamp without time zone NOT NULL,
    min_order_value numeric(14,2),
    max_discount numeric(14,2),
    usage_limit integer,
    usage_per_user integer,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    promotion_type character varying(30) NOT NULL,
    status character varying(20) NOT NULL,
    points_required integer,
    expiry_reminded_at timestamp without time zone,
    CONSTRAINT chk_promotions_discount_type CHECK (((discount_type)::text = ANY ((ARRAY['PERCENT'::character varying, 'FIXED'::character varying])::text[]))),
    CONSTRAINT chk_promotions_discount_value CHECK (((discount_value > (0)::numeric) AND (((discount_type)::text <> 'PERCENT'::text) OR (discount_value <= (100)::numeric)))),
    CONSTRAINT chk_promotions_period CHECK ((end_date > start_date)),
    CONSTRAINT chk_promotions_points CHECK (((((promotion_type)::text = 'POINT_REDEMPTION'::text) AND (points_required > 0)) OR (((promotion_type)::text <> 'POINT_REDEMPTION'::text) AND (points_required IS NULL)))),
    CONSTRAINT chk_promotions_status CHECK (((status)::text = ANY ((ARRAY['SCHEDULED'::character varying, 'ACTIVE'::character varying, 'INACTIVE'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT chk_promotions_type CHECK (((promotion_type)::text = ANY ((ARRAY['ORDER'::character varying, 'PRODUCT'::character varying, 'POINT_REDEMPTION'::character varying])::text[])))
);

--
-- Name: TABLE promotions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.promotions IS 'Main promotion table';

--
-- Name: refunds; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refunds (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    payment_id uuid NOT NULL,
    refund_code character varying(50),
    amount numeric(14,2) NOT NULL,
    reason text,
    status character varying(50) NOT NULL,
    requested_by uuid,
    requested_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    processed_at timestamp without time zone,
    order_id uuid,
    CONSTRAINT refunds_amount_check CHECK ((amount >= (0)::numeric))
);

--
-- Name: TABLE refunds; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.refunds IS 'Store refund requests and refund processing information';

--
-- Name: revoked_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.revoked_tokens (
    token_id uuid NOT NULL,
    account_id uuid NOT NULL,
    account_type character varying(20) NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    revoked_at timestamp(6) without time zone NOT NULL
);

--
-- Name: role_permission_audit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role_permission_audit (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    role_id uuid,
    permission_id uuid,
    action character varying(20) NOT NULL,
    changed_by uuid,
    changed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

--
-- Name: role_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role_permissions (
    role_id uuid NOT NULL,
    permission_id uuid NOT NULL,
    scope character varying(20) NOT NULL,
    CONSTRAINT chk_role_permissions_scope CHECK (((scope)::text = ANY ((ARRAY['SELF'::character varying, 'TEAM'::character varying, 'DEPARTMENT'::character varying, 'STORE'::character varying, 'ALL'::character varying])::text[])))
);

--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

--
-- Name: shipments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shipments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    shipping_provider character varying(100),
    tracking_code character varying(100),
    shipping_status character varying(50) DEFAULT 'ASSIGNING_DRIVER'::character varying,
    shipped_at timestamp without time zone,
    delivered_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    attempt_no integer NOT NULL,
    created_by uuid,
    currency character varying(10),
    driver_name character varying(150),
    driver_phone character varying(30),
    driver_plate character varying(30),
    failure_reason text,
    fee numeric(14,2),
    provider_order_id character varying(64),
    provider_status character varying(40),
    provider_updated_at timestamp(6) without time zone,
    quotation_id character varying(64),
    raw_response jsonb,
    replaced_by_shipment_id uuid,
    tracking_url text,
    CONSTRAINT chk_shipments_status CHECK (((shipping_status)::text = ANY ((ARRAY['ASSIGNING_DRIVER'::character varying, 'DRIVER_ASSIGNED'::character varying, 'PICKED_UP'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'CANCELLED'::character varying, 'REPLACED'::character varying])::text[])))
);

--
-- Name: TABLE shipments; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.shipments IS 'Stores shipment information';

--
-- Name: shipping_webhook_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shipping_webhook_events (
    id uuid NOT NULL,
    event_id character varying(100) NOT NULL,
    event_type character varying(50) NOT NULL,
    payload jsonb NOT NULL,
    provider character varying(30) NOT NULL,
    provider_order_id character varying(64),
    received_at timestamp(6) without time zone NOT NULL,
    result character varying(40) NOT NULL
);

--
-- Name: stock_reservations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stock_reservations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    store_id uuid NOT NULL,
    product_variant_id uuid NOT NULL,
    quantity integer NOT NULL,
    status character varying(30) DEFAULT 'ACTIVE'::character varying,
    expired_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone
);

--
-- Name: store_staffs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.store_staffs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    store_id uuid NOT NULL,
    staff_role character varying(50),
    start_date date,
    end_date date,
    active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

--
-- Name: stores; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stores (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50),
    name character varying(255) NOT NULL,
    address text,
    phone character varying(20),
    latitude numeric(9,6),
    longitude numeric(9,6),
    active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    opening_hours character varying(100),
    image_url character varying(1000),
    image_public_id character varying(255),
    services text[] DEFAULT '{}'::text[] NOT NULL
);

--
-- Name: supplier_code_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.supplier_code_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: suppliers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.suppliers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50),
    name character varying(255) NOT NULL,
    contact_name character varying(255),
    phone character varying(20),
    email character varying(255),
    address text,
    status character varying(50) DEFAULT 'ACTIVE'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone
);

--
-- Name: support_faqs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.support_faqs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    topic character varying(30) NOT NULL,
    title character varying(200) NOT NULL,
    answer text NOT NULL,
    keywords character varying(500),
    sort_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    updated_by uuid,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_support_faqs_topic CHECK (((topic)::text = ANY ((ARRAY['RETURN'::character varying, 'CANCEL'::character varying, 'SHIPPING'::character varying, 'PAYMENT'::character varying])::text[])))
);

--
-- Name: user_departments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_departments (
    department_id uuid NOT NULL,
    user_id uuid NOT NULL,
    assigned_at timestamp(6) without time zone
);

--
-- Name: user_notification_preferences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_notification_preferences (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    channel character varying(20) NOT NULL,
    enabled boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone
);

--
-- Name: user_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_permissions (
    user_id uuid NOT NULL,
    permission_id uuid NOT NULL,
    effect character varying(10) NOT NULL,
    scope character varying(20) NOT NULL,
    CONSTRAINT chk_user_permissions_scope CHECK (((scope)::text = ANY ((ARRAY['SELF'::character varying, 'TEAM'::character varying, 'DEPARTMENT'::character varying, 'STORE'::character varying, 'ALL'::character varying])::text[]))),
    CONSTRAINT user_permissions_effect_check CHECK (((effect)::text = ANY ((ARRAY['ALLOW'::character varying, 'DENY'::character varying])::text[])))
);

--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_roles (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    assigned_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

--
-- Name: user_tiers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_tiers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    tier_id uuid NOT NULL,
    assigned_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamp without time zone,
    note text
);

--
-- Name: user_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token_hash text NOT NULL,
    token_type character varying(30) DEFAULT 'REFRESH'::character varying,
    refresh_token_family uuid,
    parent_token_id uuid,
    expires_at timestamp without time zone NOT NULL,
    revoked_at timestamp without time zone,
    device character varying(100),
    ip_address character varying(50),
    user_agent text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(255),
    phone character varying(20),
    password_hash text,
    active boolean DEFAULT true,
    locked boolean DEFAULT false,
    failed_login_attempts integer DEFAULT 0,
    last_password_change timestamp without time zone,
    email_verified boolean DEFAULT false,
    phone_verified boolean DEFAULT false,
    last_login timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    deleted_at timestamp without time zone,
    avatar text,
    date_of_birth date,
    employee_code character varying(30),
    employment_status character varying(30),
    employment_type character varying(30),
    full_name character varying(255),
    gender character varying(20),
    hire_date date,
    job_title character varying(150),
    manager_id uuid,
    termination_date date,
    username character varying(50) NOT NULL,
    work_location character varying(150),
    position_id uuid,
    login_locked_until timestamp(6) without time zone
);

--
-- Name: wishlist_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.wishlist_items (
    customer_id uuid NOT NULL,
    product_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT (now() AT TIME ZONE 'UTC'::text) NOT NULL
);

--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);

--
-- Name: audit_outbox audit_outbox_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_outbox
    ADD CONSTRAINT audit_outbox_pkey PRIMARY KEY (id);

--
-- Name: auth_audit_logs auth_audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_audit_logs
    ADD CONSTRAINT auth_audit_logs_pkey PRIMARY KEY (id);

--
-- Name: brands brands_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brands
    ADD CONSTRAINT brands_code_key UNIQUE (code);

--
-- Name: brands brands_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brands
    ADD CONSTRAINT brands_pkey PRIMARY KEY (id);

--
-- Name: cart_items cart_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_pkey PRIMARY KEY (id);

--
-- Name: categories categories_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_code_key UNIQUE (code);

--
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);

--
-- Name: chat_attachments chat_attachments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_attachments
    ADD CONSTRAINT chat_attachments_pkey PRIMARY KEY (id);

--
-- Name: goods_issue_items ck_warehouse_issue_item_quantity; Type: CHECK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE public.goods_issue_items
    ADD CONSTRAINT ck_warehouse_issue_item_quantity CHECK ((quantity > 0)) NOT VALID;

--
-- Name: goods_issues ck_warehouse_issue_reason; Type: CHECK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE public.goods_issues
    ADD CONSTRAINT ck_warehouse_issue_reason CHECK ((((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'CANCELLED'::character varying])::text[])) OR ((issue_type)::text <> 'OTHER'::text) OR (NULLIF(btrim(reason), ''::text) IS NOT NULL))) NOT VALID;

--
-- Name: goods_issues ck_warehouse_issue_supplier; Type: CHECK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE public.goods_issues
    ADD CONSTRAINT ck_warehouse_issue_supplier CHECK ((((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'CANCELLED'::character varying])::text[])) OR ((issue_type)::text <> 'RETURN_TO_SUPPLIER'::text) OR (supplier_id IS NOT NULL))) NOT VALID;

--
-- Name: goods_receipt_items ck_warehouse_receipt_item_cost; Type: CHECK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE public.goods_receipt_items
    ADD CONSTRAINT ck_warehouse_receipt_item_cost CHECK ((cost_price >= (0)::numeric)) NOT VALID;

--
-- Name: goods_receipt_items ck_warehouse_receipt_item_quantity; Type: CHECK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE public.goods_receipt_items
    ADD CONSTRAINT ck_warehouse_receipt_item_quantity CHECK ((quantity > 0)) NOT VALID;

--
-- Name: goods_receipts ck_warehouse_receipt_supplier; Type: CHECK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE public.goods_receipts
    ADD CONSTRAINT ck_warehouse_receipt_supplier CHECK ((((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'CANCELLED'::character varying])::text[])) OR (supplier_id IS NOT NULL))) NOT VALID;

--
-- Name: collections collections_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collections
    ADD CONSTRAINT collections_code_key UNIQUE (code);

--
-- Name: collections collections_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collections
    ADD CONSTRAINT collections_pkey PRIMARY KEY (id);

--
-- Name: customer_accounts customer_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_accounts
    ADD CONSTRAINT customer_accounts_pkey PRIMARY KEY (id);

--
-- Name: customer_activity_logs customer_activity_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_activity_logs
    ADD CONSTRAINT customer_activity_logs_pkey PRIMARY KEY (id);

--
-- Name: customer_addresses customer_addresses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_addresses
    ADD CONSTRAINT customer_addresses_pkey PRIMARY KEY (id);

--
-- Name: customer_password_reset_challenges customer_password_reset_challenges_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_password_reset_challenges
    ADD CONSTRAINT customer_password_reset_challenges_pkey PRIMARY KEY (id);

--
-- Name: customer_profiles customer_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_profiles
    ADD CONSTRAINT customer_profiles_pkey PRIMARY KEY (user_id);

--
-- Name: customer_promotions customer_promotions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_promotions
    ADD CONSTRAINT customer_promotions_pkey PRIMARY KEY (id);

--
-- Name: customer_refresh_tokens customer_refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_refresh_tokens
    ADD CONSTRAINT customer_refresh_tokens_pkey PRIMARY KEY (id);

--
-- Name: customer_social_accounts customer_social_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_social_accounts
    ADD CONSTRAINT customer_social_accounts_pkey PRIMARY KEY (id);

--
-- Name: customer_tier_assignments customer_tier_assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_tier_assignments
    ADD CONSTRAINT customer_tier_assignments_pkey PRIMARY KEY (id);

--
-- Name: customer_tiers customer_tiers_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_tiers
    ADD CONSTRAINT customer_tiers_code_key UNIQUE (code);

--
-- Name: customer_tiers customer_tiers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_tiers
    ADD CONSTRAINT customer_tiers_pkey PRIMARY KEY (id);

--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (id);

--
-- Name: departments departments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_pkey PRIMARY KEY (id);

--
-- Name: goods_issue_items goods_issue_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_issue_items
    ADD CONSTRAINT goods_issue_items_pkey PRIMARY KEY (id);

--
-- Name: goods_issues goods_issues_issue_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_issues
    ADD CONSTRAINT goods_issues_issue_code_key UNIQUE (issue_code);

--
-- Name: goods_issues goods_issues_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_issues
    ADD CONSTRAINT goods_issues_pkey PRIMARY KEY (id);

--
-- Name: goods_receipt_items goods_receipt_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_receipt_items
    ADD CONSTRAINT goods_receipt_items_pkey PRIMARY KEY (id);

--
-- Name: goods_receipts goods_receipts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_receipts
    ADD CONSTRAINT goods_receipts_pkey PRIMARY KEY (id);

--
-- Name: goods_receipts goods_receipts_receipt_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_receipts
    ADD CONSTRAINT goods_receipts_receipt_code_key UNIQUE (receipt_code);

--
-- Name: internal_chat_messages internal_chat_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.internal_chat_messages
    ADD CONSTRAINT internal_chat_messages_pkey PRIMARY KEY (id);

--
-- Name: internal_chat_rooms internal_chat_rooms_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.internal_chat_rooms
    ADD CONSTRAINT internal_chat_rooms_pkey PRIMARY KEY (id);

--
-- Name: inventory_balances inventory_balances_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_balances
    ADD CONSTRAINT inventory_balances_pkey PRIMARY KEY (store_id, product_variant_id);

--
-- Name: inventory_transactions inventory_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_transactions
    ADD CONSTRAINT inventory_transactions_pkey PRIMARY KEY (id);

--
-- Name: loyalty_accounts loyalty_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_accounts
    ADD CONSTRAINT loyalty_accounts_pkey PRIMARY KEY (customer_id);

--
-- Name: loyalty_transactions loyalty_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_transactions
    ADD CONSTRAINT loyalty_transactions_pkey PRIMARY KEY (id);

--
-- Name: modules modules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.modules
    ADD CONSTRAINT modules_pkey PRIMARY KEY (id);

--
-- Name: notification_logs notification_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_logs
    ADD CONSTRAINT notification_logs_pkey PRIMARY KEY (id);

--
-- Name: notification_templates notification_templates_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_templates
    ADD CONSTRAINT notification_templates_code_key UNIQUE (code);

--
-- Name: notification_templates notification_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_templates
    ADD CONSTRAINT notification_templates_pkey PRIMARY KEY (id);

--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);

--
-- Name: order_addresses order_addresses_order_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_addresses
    ADD CONSTRAINT order_addresses_order_id_key UNIQUE (order_id);

--
-- Name: order_addresses order_addresses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_addresses
    ADD CONSTRAINT order_addresses_pkey PRIMARY KEY (id);

--
-- Name: order_chat_message_status order_chat_message_status_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_chat_message_status
    ADD CONSTRAINT order_chat_message_status_pkey PRIMARY KEY (message_id, user_id);

--
-- Name: order_chat_messages order_chat_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_chat_messages
    ADD CONSTRAINT order_chat_messages_pkey PRIMARY KEY (id);

--
-- Name: order_chat_rooms order_chat_rooms_order_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_chat_rooms
    ADD CONSTRAINT order_chat_rooms_order_id_key UNIQUE (order_id);

--
-- Name: order_chat_rooms order_chat_rooms_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_chat_rooms
    ADD CONSTRAINT order_chat_rooms_pkey PRIMARY KEY (id);

--
-- Name: order_customer_contacts order_customer_contacts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_customer_contacts
    ADD CONSTRAINT order_customer_contacts_pkey PRIMARY KEY (id);

--
-- Name: order_issue_types order_issue_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_issue_types
    ADD CONSTRAINT order_issue_types_pkey PRIMARY KEY (code);

--
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);

--
-- Name: order_promotions order_promotions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_promotions
    ADD CONSTRAINT order_promotions_pkey PRIMARY KEY (id);

--
-- Name: order_status_histories order_status_histories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_status_histories
    ADD CONSTRAINT order_status_histories_pkey PRIMARY KEY (id);

--
-- Name: orders orders_order_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_order_code_key UNIQUE (order_code);

--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);

--
-- Name: password_reset_otps password_reset_otps_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_otps
    ADD CONSTRAINT password_reset_otps_pkey PRIMARY KEY (id);

--
-- Name: payment_transactions payment_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_transactions
    ADD CONSTRAINT payment_transactions_pkey PRIMARY KEY (id);

--
-- Name: payment_webhook_logs payment_webhook_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_webhook_logs
    ADD CONSTRAINT payment_webhook_logs_pkey PRIMARY KEY (id);

--
-- Name: payments payments_payment_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_payment_code_key UNIQUE (payment_code);

--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);

--
-- Name: permission_groups permission_groups_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permission_groups
    ADD CONSTRAINT permission_groups_code_key UNIQUE (code);

--
-- Name: permission_groups permission_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permission_groups
    ADD CONSTRAINT permission_groups_pkey PRIMARY KEY (id);

--
-- Name: permissions permissions_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_code_key UNIQUE (code);

--
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);

--
-- Name: positions positions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.positions
    ADD CONSTRAINT positions_pkey PRIMARY KEY (id);

--
-- Name: product_attributes product_attributes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_attributes
    ADD CONSTRAINT product_attributes_pkey PRIMARY KEY (id);

--
-- Name: product_images product_images_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_images
    ADD CONSTRAINT product_images_pkey PRIMARY KEY (id);

--
-- Name: product_tag_mappings product_tag_mappings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_tag_mappings
    ADD CONSTRAINT product_tag_mappings_pkey PRIMARY KEY (product_id, tag_id);

--
-- Name: product_tags product_tags_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_tags
    ADD CONSTRAINT product_tags_name_key UNIQUE (name);

--
-- Name: product_tags product_tags_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_tags
    ADD CONSTRAINT product_tags_pkey PRIMARY KEY (id);

--
-- Name: product_variants product_variants_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_variants
    ADD CONSTRAINT product_variants_pkey PRIMARY KEY (id);

--
-- Name: product_variants product_variants_sku_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_variants
    ADD CONSTRAINT product_variants_sku_key UNIQUE (sku);

--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);

--
-- Name: products products_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_slug_key UNIQUE (slug);

--
-- Name: promotion_conditions promotion_conditions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotion_conditions
    ADD CONSTRAINT promotion_conditions_pkey PRIMARY KEY (id);

--
-- Name: promotion_products promotion_products_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotion_products
    ADD CONSTRAINT promotion_products_pkey PRIMARY KEY (promotion_id, product_id);

--
-- Name: promotion_tiers promotion_tiers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotion_tiers
    ADD CONSTRAINT promotion_tiers_pkey PRIMARY KEY (promotion_id, tier_id);

--
-- Name: promotion_usages promotion_usages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotion_usages
    ADD CONSTRAINT promotion_usages_pkey PRIMARY KEY (id);

--
-- Name: promotions promotions_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotions
    ADD CONSTRAINT promotions_code_key UNIQUE (code);

--
-- Name: promotions promotions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotions
    ADD CONSTRAINT promotions_pkey PRIMARY KEY (id);

--
-- Name: refunds refunds_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refunds
    ADD CONSTRAINT refunds_pkey PRIMARY KEY (id);

--
-- Name: refunds refunds_refund_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refunds
    ADD CONSTRAINT refunds_refund_code_key UNIQUE (refund_code);

--
-- Name: revoked_tokens revoked_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.revoked_tokens
    ADD CONSTRAINT revoked_tokens_pkey PRIMARY KEY (token_id);

--
-- Name: role_permission_audit role_permission_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permission_audit
    ADD CONSTRAINT role_permission_audit_pkey PRIMARY KEY (id);

--
-- Name: role_permissions role_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id);

--
-- Name: roles roles_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_code_key UNIQUE (code);

--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);

--
-- Name: shipments shipments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipments
    ADD CONSTRAINT shipments_pkey PRIMARY KEY (id);

--
-- Name: shipping_webhook_events shipping_webhook_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipping_webhook_events
    ADD CONSTRAINT shipping_webhook_events_pkey PRIMARY KEY (id);

--
-- Name: stock_reservations stock_reservations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_reservations
    ADD CONSTRAINT stock_reservations_pkey PRIMARY KEY (id);

--
-- Name: store_staffs store_staffs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.store_staffs
    ADD CONSTRAINT store_staffs_pkey PRIMARY KEY (id);

--
-- Name: stores stores_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stores
    ADD CONSTRAINT stores_code_key UNIQUE (code);

--
-- Name: stores stores_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stores
    ADD CONSTRAINT stores_pkey PRIMARY KEY (id);

--
-- Name: suppliers suppliers_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.suppliers
    ADD CONSTRAINT suppliers_code_key UNIQUE (code);

--
-- Name: suppliers suppliers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.suppliers
    ADD CONSTRAINT suppliers_pkey PRIMARY KEY (id);

--
-- Name: support_faqs support_faqs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.support_faqs
    ADD CONSTRAINT support_faqs_pkey PRIMARY KEY (id);

--
-- Name: products uk57ivhy5aj3qfmdvl6vxdfjs4p; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT uk57ivhy5aj3qfmdvl6vxdfjs4p UNIQUE (code);

--
-- Name: customer_accounts uk5rjnf1jqbgo9xxvtw2jwdmos5; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_accounts
    ADD CONSTRAINT uk5rjnf1jqbgo9xxvtw2jwdmos5 UNIQUE (customer_id);

--
-- Name: customer_accounts uk8snpy8d3keycirov8ftjohoki; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_accounts
    ADD CONSTRAINT uk8snpy8d3keycirov8ftjohoki UNIQUE (username);

--
-- Name: audit_outbox uk9vl818xhbifefr701s8xqjgk3; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_outbox
    ADD CONSTRAINT uk9vl818xhbifefr701s8xqjgk3 UNIQUE (event_id);

--
-- Name: cart_items uk_cart_items_customer_variant; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT uk_cart_items_customer_variant UNIQUE (customer_id, product_variant_id);

--
-- Name: user_notification_preferences uk_user_notification_channel; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_notification_preferences
    ADD CONSTRAINT uk_user_notification_channel UNIQUE (user_id, channel);

--
-- Name: positions ukdjkia0ifarv9epmv78bh62r3o; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.positions
    ADD CONSTRAINT ukdjkia0ifarv9epmv78bh62r3o UNIQUE (code);

--
-- Name: audit_logs ukfamedw8vl6xpi5l5b5l7320kv; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT ukfamedw8vl6xpi5l5b5l7320kv UNIQUE (event_id);

--
-- Name: customers ukiqv746oh5t5is1vr4p2nl79r6; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT ukiqv746oh5t5is1vr4p2nl79r6 UNIQUE (customer_code);

--
-- Name: customer_refresh_tokens ukky7lfwhoutao6givttjf7u36c; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_refresh_tokens
    ADD CONSTRAINT ukky7lfwhoutao6givttjf7u36c UNIQUE (token_hash);

--
-- Name: departments ukl7tivi5261wxdnvo6cct9gg6t; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT ukl7tivi5261wxdnvo6cct9gg6t UNIQUE (code);

--
-- Name: customers ukm3iom37efaxd5eucmxjqqcbe9; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT ukm3iom37efaxd5eucmxjqqcbe9 UNIQUE (phone);

--
-- Name: users ukob2dx53vn966ek0roock2539f; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT ukob2dx53vn966ek0roock2539f UNIQUE (employee_code);

--
-- Name: users ukr43af9ap4edm43mmtq01oddj6; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT ukr43af9ap4edm43mmtq01oddj6 UNIQUE (username);

--
-- Name: modules ukr6b8cuvhddsfrycjnkvy3d3ig; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.modules
    ADD CONSTRAINT ukr6b8cuvhddsfrycjnkvy3d3ig UNIQUE (code);

--
-- Name: customers ukrfbvkrffamfql7cjmen8v976v; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT ukrfbvkrffamfql7cjmen8v976v UNIQUE (email);

--
-- Name: customer_social_accounts uq_customer_social_customer_provider; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_social_accounts
    ADD CONSTRAINT uq_customer_social_customer_provider UNIQUE (customer_id, provider);

--
-- Name: customer_social_accounts uq_customer_social_provider_subject; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_social_accounts
    ADD CONSTRAINT uq_customer_social_provider_subject UNIQUE (provider, provider_user_id);

--
-- Name: stores uq_stores_coordinates; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stores
    ADD CONSTRAINT uq_stores_coordinates UNIQUE (latitude, longitude);

--
-- Name: user_departments user_departments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_departments
    ADD CONSTRAINT user_departments_pkey PRIMARY KEY (department_id, user_id);

--
-- Name: user_notification_preferences user_notification_preferences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_notification_preferences
    ADD CONSTRAINT user_notification_preferences_pkey PRIMARY KEY (id);

--
-- Name: user_permissions user_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_permissions
    ADD CONSTRAINT user_permissions_pkey PRIMARY KEY (user_id, permission_id);

--
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);

--
-- Name: user_tiers user_tiers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_tiers
    ADD CONSTRAINT user_tiers_pkey PRIMARY KEY (id);

--
-- Name: user_tokens user_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_tokens
    ADD CONSTRAINT user_tokens_pkey PRIMARY KEY (id);

--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);

--
-- Name: users users_phone_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_phone_key UNIQUE (phone);

--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

--
-- Name: wishlist_items wishlist_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wishlist_items
    ADD CONSTRAINT wishlist_items_pkey PRIMARY KEY (customer_id, product_id);

--
-- Name: idx_cart_items_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cart_items_customer ON public.cart_items USING btree (customer_id, created_at);

--
-- Name: idx_categories_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_categories_active ON public.categories USING btree (active);

--
-- Name: idx_chat_attachments_message_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_chat_attachments_message_id ON public.chat_attachments USING btree (message_id);

--
-- Name: idx_collections_brand_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collections_brand_id ON public.collections USING btree (brand_id);

--
-- Name: idx_customer_activity_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customer_activity_created_at ON public.customer_activity_logs USING btree (created_at);

--
-- Name: idx_customer_activity_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customer_activity_entity ON public.customer_activity_logs USING btree (entity_type, entity_id);

--
-- Name: idx_customer_activity_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customer_activity_user ON public.customer_activity_logs USING btree (user_id);

--
-- Name: idx_customer_addresses_default; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customer_addresses_default ON public.customer_addresses USING btree (user_id, is_default);

--
-- Name: idx_customer_addresses_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customer_addresses_user_id ON public.customer_addresses USING btree (user_id);

--
-- Name: idx_customer_promotions_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customer_promotions_customer ON public.customer_promotions USING btree (customer_id, redeemed_at DESC);

--
-- Name: idx_customer_promotions_promotion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customer_promotions_promotion ON public.customer_promotions USING btree (promotion_id);

--
-- Name: idx_customer_promotions_unused; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customer_promotions_unused ON public.customer_promotions USING btree (customer_id, promotion_id) WHERE (used_order_id IS NULL);

--
-- Name: idx_customer_tier_assignments_tier; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customer_tier_assignments_tier ON public.customer_tier_assignments USING btree (tier_id);

--
-- Name: idx_customers_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customers_active ON public.customers USING btree (active);

--
-- Name: idx_customers_origin_store; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customers_origin_store ON public.customers USING btree (origin_store_id);

--
-- Name: idx_customers_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customers_source ON public.customers USING btree (source);

--
-- Name: idx_goods_issue_items_issue_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_goods_issue_items_issue_id ON public.goods_issue_items USING btree (issue_id);

--
-- Name: idx_goods_issues_order_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_goods_issues_order_id ON public.goods_issues USING btree (order_id);

--
-- Name: idx_goods_issues_store_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_goods_issues_store_id ON public.goods_issues USING btree (store_id);

--
-- Name: idx_goods_receipt_items_receipt_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_goods_receipt_items_receipt_id ON public.goods_receipt_items USING btree (receipt_id);

--
-- Name: idx_goods_receipt_items_variant_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_goods_receipt_items_variant_id ON public.goods_receipt_items USING btree (product_variant_id);

--
-- Name: idx_goods_receipts_store_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_goods_receipts_store_id ON public.goods_receipts USING btree (store_id);

--
-- Name: idx_goods_receipts_supplier_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_goods_receipts_supplier_id ON public.goods_receipts USING btree (supplier_id);

--
-- Name: idx_internal_chat_messages_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_internal_chat_messages_created_at ON public.internal_chat_messages USING btree (created_at DESC);

--
-- Name: idx_internal_chat_messages_room_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_internal_chat_messages_room_id ON public.internal_chat_messages USING btree (room_id);

--
-- Name: idx_inventory_balances_variant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inventory_balances_variant ON public.inventory_balances USING btree (product_variant_id);

--
-- Name: idx_inventory_balances_variant_offline; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inventory_balances_variant_offline ON public.inventory_balances USING btree (product_variant_id) WHERE (available_quantity > 0);

--
-- Name: idx_inventory_transactions_variant_store; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inventory_transactions_variant_store ON public.inventory_transactions USING btree (product_variant_id, store_id);

--
-- Name: idx_loyalty_transactions_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loyalty_transactions_customer ON public.loyalty_transactions USING btree (customer_id);

--
-- Name: idx_loyalty_transactions_customer_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loyalty_transactions_customer_created ON public.loyalty_transactions USING btree (customer_id, created_at DESC);

--
-- Name: idx_loyalty_transactions_reference; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loyalty_transactions_reference ON public.loyalty_transactions USING btree (reference_type, reference_id);

--
-- Name: idx_loyalty_transactions_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loyalty_transactions_type ON public.loyalty_transactions USING btree (transaction_type);

--
-- Name: idx_loyalty_transactions_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loyalty_transactions_user_id ON public.loyalty_transactions USING btree (user_id);

--
-- Name: idx_notification_logs_channel; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notification_logs_channel ON public.notification_logs USING btree (channel);

--
-- Name: idx_notification_logs_notification_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notification_logs_notification_id ON public.notification_logs USING btree (notification_id);

--
-- Name: idx_notification_logs_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notification_logs_status ON public.notification_logs USING btree (status);

--
-- Name: idx_notification_templates_channel; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notification_templates_channel ON public.notification_templates USING btree (channel);

--
-- Name: idx_notification_templates_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notification_templates_code ON public.notification_templates USING btree (code);

--
-- Name: idx_notifications_channel; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_channel ON public.notifications USING btree (channel);

--
-- Name: idx_notifications_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_created_at ON public.notifications USING btree (created_at);

--
-- Name: idx_notifications_recipient_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_recipient_created ON public.notifications USING btree (recipient_type, recipient_id, created_at DESC);

--
-- Name: idx_notifications_recipient_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_recipient_id ON public.notifications USING btree (recipient_id);

--
-- Name: idx_notifications_recipient_unread; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_recipient_unread ON public.notifications USING btree (recipient_type, recipient_id) WHERE (read_at IS NULL);

--
-- Name: idx_notifications_reference; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_reference ON public.notifications USING btree (reference_type, reference_id);

--
-- Name: idx_notifications_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_status ON public.notifications USING btree (status);

--
-- Name: idx_order_chat_messages_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_chat_messages_created_at ON public.order_chat_messages USING btree (created_at DESC);

--
-- Name: idx_order_chat_messages_room; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_chat_messages_room ON public.order_chat_messages USING btree (room_id, created_at);

--
-- Name: idx_order_chat_messages_room_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_chat_messages_room_id ON public.order_chat_messages USING btree (room_id);

--
-- Name: idx_order_chat_room_customer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_chat_room_customer_id ON public.order_chat_rooms USING btree (customer_id);

--
-- Name: idx_order_chat_room_order_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_chat_room_order_id ON public.order_chat_rooms USING btree (order_id);

--
-- Name: idx_order_chat_room_staff_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_chat_room_staff_id ON public.order_chat_rooms USING btree (assigned_staff_id);

--
-- Name: idx_order_chat_rooms_customer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_chat_rooms_customer_id ON public.order_chat_rooms USING btree (customer_id);

--
-- Name: idx_order_chat_rooms_staff_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_chat_rooms_staff_id ON public.order_chat_rooms USING btree (assigned_staff_id);

--
-- Name: idx_order_chat_rooms_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_chat_rooms_status ON public.order_chat_rooms USING btree (status, last_message_at DESC);

--
-- Name: idx_order_customer_contacts_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_customer_contacts_order ON public.order_customer_contacts USING btree (order_id, created_at DESC);

--
-- Name: idx_order_items_order_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_items_order_id ON public.order_items USING btree (order_id);

--
-- Name: idx_order_items_variant_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_items_variant_id ON public.order_items USING btree (product_variant_id);

--
-- Name: idx_order_promotions_order_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_promotions_order_id ON public.order_promotions USING btree (order_id);

--
-- Name: idx_order_status_histories_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_status_histories_order ON public.order_status_histories USING btree (order_id, changed_at);

--
-- Name: idx_order_status_histories_order_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_status_histories_order_id ON public.order_status_histories USING btree (order_id);

--
-- Name: idx_orders_awaiting_payment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_awaiting_payment ON public.orders USING btree (payment_due_at) WHERE (((payment_status)::text = 'AWAITING_PAYMENT'::text) AND (parent_order_id IS NULL));

--
-- Name: idx_orders_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_customer ON public.orders USING btree (customer_id);

--
-- Name: idx_orders_handler; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_handler ON public.orders USING btree (handler_user_id);

--
-- Name: idx_orders_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_parent ON public.orders USING btree (parent_order_id);

--
-- Name: idx_orders_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_status ON public.orders USING btree (status);

--
-- Name: idx_orders_store_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_store_id ON public.orders USING btree (store_id);

--
-- Name: idx_orders_store_type_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_store_type_created ON public.orders USING btree (store_id, order_type, active, created_at DESC);

--
-- Name: idx_orders_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_user_id ON public.orders USING btree (user_id);

--
-- Name: idx_orders_user_store; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_user_store ON public.orders USING btree (user_id, store_id);

--
-- Name: idx_payment_transactions_gateway_transaction_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_transactions_gateway_transaction_id ON public.payment_transactions USING btree (gateway_transaction_id);

--
-- Name: idx_payment_transactions_payment_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_transactions_payment_id ON public.payment_transactions USING btree (payment_id);

--
-- Name: idx_payment_transactions_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_transactions_status ON public.payment_transactions USING btree (status);

--
-- Name: idx_payment_webhook_logs_payment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_webhook_logs_payment ON public.payment_webhook_logs USING btree (payment_id);

--
-- Name: idx_payment_webhook_logs_processed; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_webhook_logs_processed ON public.payment_webhook_logs USING btree (processed);

--
-- Name: idx_payment_webhook_logs_provider; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_webhook_logs_provider ON public.payment_webhook_logs USING btree (provider);

--
-- Name: idx_payments_order_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_order_id ON public.payments USING btree (order_id);

--
-- Name: idx_payments_order_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_order_status ON public.payments USING btree (order_id, status);

--
-- Name: idx_payments_payment_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_payment_code ON public.payments USING btree (payment_code);

--
-- Name: idx_payments_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_status ON public.payments USING btree (status);

--
-- Name: idx_permissions_group_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_permissions_group_id ON public.permissions USING btree (group_id);

--
-- Name: idx_product_attributes_product_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_product_attributes_product_id ON public.product_attributes USING btree (product_id);

--
-- Name: idx_product_images_variant_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_product_images_variant_id ON public.product_images USING btree (product_variant_id);

--
-- Name: idx_product_variants_product_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_product_variants_product_id ON public.product_variants USING btree (product_id);

--
-- Name: idx_products_brand_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_products_brand_id ON public.products USING btree (brand_id);

--
-- Name: idx_products_category_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_products_category_id ON public.products USING btree (category_id);

--
-- Name: idx_products_collection_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_products_collection_id ON public.products USING btree (collection_id);

--
-- Name: idx_promotion_conditions_promotion_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotion_conditions_promotion_id ON public.promotion_conditions USING btree (promotion_id);

--
-- Name: idx_promotion_products_product; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotion_products_product ON public.promotion_products USING btree (product_id);

--
-- Name: idx_promotion_tiers_tier; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotion_tiers_tier ON public.promotion_tiers USING btree (tier_id);

--
-- Name: idx_promotion_usages_order_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotion_usages_order_id ON public.promotion_usages USING btree (order_id);

--
-- Name: idx_promotion_usages_promotion_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotion_usages_promotion_customer ON public.promotion_usages USING btree (promotion_id, customer_id);

--
-- Name: idx_promotion_usages_promotion_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotion_usages_promotion_id ON public.promotion_usages USING btree (promotion_id);

--
-- Name: idx_promotion_usages_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotion_usages_user_id ON public.promotion_usages USING btree (user_id);

--
-- Name: idx_promotions_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotions_code ON public.promotions USING btree (code);

--
-- Name: idx_promotions_end_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotions_end_date ON public.promotions USING btree (end_date);

--
-- Name: idx_promotions_start_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotions_start_date ON public.promotions USING btree (start_date);

--
-- Name: idx_promotions_status_end; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotions_status_end ON public.promotions USING btree (status, end_date);

--
-- Name: idx_promotions_status_start; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotions_status_start ON public.promotions USING btree (status, start_date);

--
-- Name: idx_refunds_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refunds_order ON public.refunds USING btree (order_id);

--
-- Name: idx_refunds_payment_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refunds_payment_id ON public.refunds USING btree (payment_id);

--
-- Name: idx_refunds_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refunds_status ON public.refunds USING btree (status);

--
-- Name: idx_shipments_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_shipments_order ON public.shipments USING btree (order_id, created_at DESC);

--
-- Name: idx_shipments_order_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_shipments_order_id ON public.shipments USING btree (order_id);

--
-- Name: idx_shipping_webhook_events_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_shipping_webhook_events_order ON public.shipping_webhook_events USING btree (provider, provider_order_id);

--
-- Name: idx_stock_reservations_order_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_stock_reservations_order_id ON public.stock_reservations USING btree (order_id);

--
-- Name: idx_stock_reservations_variant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_stock_reservations_variant ON public.stock_reservations USING btree (product_variant_id);

--
-- Name: idx_store_staffs_store_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_store_staffs_store_id ON public.store_staffs USING btree (store_id);

--
-- Name: idx_store_staffs_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_store_staffs_user_id ON public.store_staffs USING btree (user_id);

--
-- Name: idx_tags_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tags_active ON public.product_tags USING btree (active);

--
-- Name: idx_user_notification_preferences_channel; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_notification_preferences_channel ON public.user_notification_preferences USING btree (channel);

--
-- Name: idx_user_notification_preferences_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_notification_preferences_user_id ON public.user_notification_preferences USING btree (user_id);

--
-- Name: idx_user_tiers_tier_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_tiers_tier_id ON public.user_tiers USING btree (tier_id);

--
-- Name: idx_user_tiers_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_tiers_user_id ON public.user_tiers USING btree (user_id);

--
-- Name: idx_user_tokens_family; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_tokens_family ON public.user_tokens USING btree (refresh_token_family);

--
-- Name: idx_user_tokens_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_tokens_user_id ON public.user_tokens USING btree (user_id);

--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_email ON public.users USING btree (email);

--
-- Name: idx_users_phone; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_phone ON public.users USING btree (phone);

--
-- Name: idx_warehouse_balance_variant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouse_balance_variant ON public.inventory_balances USING btree (product_variant_id);

--
-- Name: idx_warehouse_issue_completed_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouse_issue_completed_by ON public.goods_issues USING btree (completed_by);

--
-- Name: idx_warehouse_issue_item_product; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouse_issue_item_product ON public.goods_issue_items USING btree (product_id);

--
-- Name: idx_warehouse_issue_store_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouse_issue_store_status ON public.goods_issues USING btree (store_id, status);

--
-- Name: idx_warehouse_issue_supplier; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouse_issue_supplier ON public.goods_issues USING btree (supplier_id);

--
-- Name: idx_warehouse_receipt_completed_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouse_receipt_completed_by ON public.goods_receipts USING btree (completed_by);

--
-- Name: idx_warehouse_receipt_item_product; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouse_receipt_item_product ON public.goods_receipt_items USING btree (product_id);

--
-- Name: idx_warehouse_receipt_store_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouse_receipt_store_status ON public.goods_receipts USING btree (store_id, status);

--
-- Name: idx_warehouse_transaction_export; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouse_transaction_export ON public.inventory_transactions USING btree (export_receipt_id);

--
-- Name: idx_warehouse_transaction_import; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouse_transaction_import ON public.inventory_transactions USING btree (import_receipt_id);

--
-- Name: idx_warehouse_transaction_store_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouse_transaction_store_created ON public.inventory_transactions USING btree (store_id, created_at DESC);

--
-- Name: idx_warehouse_transaction_variant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouse_transaction_variant ON public.inventory_transactions USING btree (product_variant_id);

--
-- Name: idx_wishlist_items_customer_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_wishlist_items_customer_created ON public.wishlist_items USING btree (customer_id, created_at DESC);

--
-- Name: uk_notifications_event_recipient; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_notifications_event_recipient ON public.notifications USING btree (event_id, recipient_type, recipient_id);

--
-- Name: uk_payment_webhook_logs_event; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_payment_webhook_logs_event ON public.payment_webhook_logs USING btree (provider, event_key) WHERE (event_key IS NOT NULL);

--
-- Name: uniq_active_tier_per_user; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uniq_active_tier_per_user ON public.user_tiers USING btree (user_id) WHERE (expires_at IS NULL);

--
-- Name: uniq_default_address_per_user; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uniq_default_address_per_user ON public.customer_addresses USING btree (user_id) WHERE (is_default = true);

--
-- Name: uq_customer_accounts_login_email; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_customer_accounts_login_email ON public.customer_accounts USING btree (lower((login_email)::text)) WHERE (login_email IS NOT NULL);

--
-- Name: uq_customer_addresses_default_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_customer_addresses_default_customer ON public.customer_addresses USING btree (customer_id) WHERE (is_default = true);

--
-- Name: uq_customer_tier_assignments_current; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_customer_tier_assignments_current ON public.customer_tier_assignments USING btree (customer_id) WHERE (expires_at IS NULL);

--
-- Name: uq_customers_customer_code; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_customers_customer_code ON public.customers USING btree (customer_code);

--
-- Name: uq_customers_email_normalized; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_customers_email_normalized ON public.customers USING btree (lower((email)::text)) WHERE (email IS NOT NULL);

--
-- Name: uq_customers_normalized_phone; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_customers_normalized_phone ON public.customers USING btree (normalized_phone) WHERE (normalized_phone IS NOT NULL);

--
-- Name: uq_loyalty_transactions_order_earn; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_loyalty_transactions_order_earn ON public.loyalty_transactions USING btree (reference_id) WHERE (((transaction_type)::text = 'EARN'::text) AND ((reference_type)::text = 'ORDER'::text));

--
-- Name: uq_orders_customer_request; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_orders_customer_request ON public.orders USING btree (customer_id, client_request_id) WHERE (client_request_id IS NOT NULL);

--
-- Name: uq_shipments_provider_order; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_shipments_provider_order ON public.shipments USING btree (shipping_provider, provider_order_id) WHERE (provider_order_id IS NOT NULL);

--
-- Name: uq_support_faqs_topic; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_support_faqs_topic ON public.support_faqs USING btree (topic);

--
-- Name: uq_warehouse_variant_product; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_warehouse_variant_product ON public.product_variants USING btree (id, product_id);

--
-- Name: cart_items cart_items_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id) ON DELETE CASCADE;

--
-- Name: cart_items cart_items_product_variant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_product_variant_id_fkey FOREIGN KEY (product_variant_id) REFERENCES public.product_variants(id) ON DELETE CASCADE;

--
-- Name: customer_promotions customer_promotions_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_promotions
    ADD CONSTRAINT customer_promotions_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id);

--
-- Name: customer_promotions customer_promotions_promotion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_promotions
    ADD CONSTRAINT customer_promotions_promotion_id_fkey FOREIGN KEY (promotion_id) REFERENCES public.promotions(id);

--
-- Name: product_attributes fk_attributes_product; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_attributes
    ADD CONSTRAINT fk_attributes_product FOREIGN KEY (product_id) REFERENCES public.products(id);

--
-- Name: role_permission_audit fk_audit_permission; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permission_audit
    ADD CONSTRAINT fk_audit_permission FOREIGN KEY (permission_id) REFERENCES public.permissions(id) ON DELETE SET NULL;

--
-- Name: role_permission_audit fk_audit_role; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permission_audit
    ADD CONSTRAINT fk_audit_role FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE SET NULL;

--
-- Name: role_permission_audit fk_audit_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permission_audit
    ADD CONSTRAINT fk_audit_user FOREIGN KEY (changed_by) REFERENCES public.users(id) ON DELETE SET NULL;

--
-- Name: auth_audit_logs fk_auth_logs_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_audit_logs
    ADD CONSTRAINT fk_auth_logs_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;

--
-- Name: categories fk_categories_parent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES public.categories(id);

--
-- Name: chat_attachments fk_chat_attachments_message; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_attachments
    ADD CONSTRAINT fk_chat_attachments_message FOREIGN KEY (message_id) REFERENCES public.order_chat_messages(id) ON DELETE CASCADE;

--
-- Name: collections fk_collections_brand; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collections
    ADD CONSTRAINT fk_collections_brand FOREIGN KEY (brand_id) REFERENCES public.brands(id);

--
-- Name: customer_activity_logs fk_customer_activity_logs_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_activity_logs
    ADD CONSTRAINT fk_customer_activity_logs_user FOREIGN KEY (user_id) REFERENCES public.users(id);

--
-- Name: customer_addresses fk_customer_addresses_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_addresses
    ADD CONSTRAINT fk_customer_addresses_customer FOREIGN KEY (customer_id) REFERENCES public.customers(id) ON DELETE CASCADE;

--
-- Name: customer_addresses fk_customer_addresses_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_addresses
    ADD CONSTRAINT fk_customer_addresses_user FOREIGN KEY (user_id) REFERENCES public.users(id);

--
-- Name: customer_profiles fk_customer_profiles_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_profiles
    ADD CONSTRAINT fk_customer_profiles_user FOREIGN KEY (user_id) REFERENCES public.users(id);

--
-- Name: customer_promotions fk_customer_promotions_used_order; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_promotions
    ADD CONSTRAINT fk_customer_promotions_used_order FOREIGN KEY (used_order_id) REFERENCES public.orders(id);

--
-- Name: customer_social_accounts fk_customer_social_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_social_accounts
    ADD CONSTRAINT fk_customer_social_customer FOREIGN KEY (customer_id) REFERENCES public.customers(id) ON DELETE CASCADE;

--
-- Name: customer_tier_assignments fk_customer_tier_assignment_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_tier_assignments
    ADD CONSTRAINT fk_customer_tier_assignment_customer FOREIGN KEY (customer_id) REFERENCES public.customers(id) ON DELETE CASCADE;

--
-- Name: customer_tier_assignments fk_customer_tier_assignment_tier; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_tier_assignments
    ADD CONSTRAINT fk_customer_tier_assignment_tier FOREIGN KEY (tier_id) REFERENCES public.customer_tiers(id);

--
-- Name: customers fk_customers_origin_store; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT fk_customers_origin_store FOREIGN KEY (origin_store_id) REFERENCES public.stores(id);

--
-- Name: goods_issue_items fk_goods_issue_item_issue; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_issue_items
    ADD CONSTRAINT fk_goods_issue_item_issue FOREIGN KEY (issue_id) REFERENCES public.goods_issues(id) ON DELETE CASCADE;

--
-- Name: goods_issue_items fk_goods_issue_items_variant; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_issue_items
    ADD CONSTRAINT fk_goods_issue_items_variant FOREIGN KEY (product_variant_id) REFERENCES public.product_variants(id);

--
-- Name: goods_issues fk_goods_issue_store; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_issues
    ADD CONSTRAINT fk_goods_issue_store FOREIGN KEY (store_id) REFERENCES public.stores(id);

--
-- Name: goods_issues fk_goods_issues_approved_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_issues
    ADD CONSTRAINT fk_goods_issues_approved_by FOREIGN KEY (approved_by) REFERENCES public.users(id);

--
-- Name: goods_issues fk_goods_issues_issued_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_issues
    ADD CONSTRAINT fk_goods_issues_issued_by FOREIGN KEY (issued_by) REFERENCES public.users(id);

--
-- Name: goods_issues fk_goods_issues_order; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_issues
    ADD CONSTRAINT fk_goods_issues_order FOREIGN KEY (order_id) REFERENCES public.orders(id);

--
-- Name: goods_receipt_items fk_goods_receipt_item_receipt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_receipt_items
    ADD CONSTRAINT fk_goods_receipt_item_receipt FOREIGN KEY (receipt_id) REFERENCES public.goods_receipts(id) ON DELETE CASCADE;

--
-- Name: goods_receipt_items fk_goods_receipt_items_variant; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_receipt_items
    ADD CONSTRAINT fk_goods_receipt_items_variant FOREIGN KEY (product_variant_id) REFERENCES public.product_variants(id);

--
-- Name: goods_receipts fk_goods_receipt_store; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_receipts
    ADD CONSTRAINT fk_goods_receipt_store FOREIGN KEY (store_id) REFERENCES public.stores(id);

--
-- Name: goods_receipts fk_goods_receipt_supplier; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_receipts
    ADD CONSTRAINT fk_goods_receipt_supplier FOREIGN KEY (supplier_id) REFERENCES public.suppliers(id);

--
-- Name: goods_receipts fk_goods_receipts_approved_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_receipts
    ADD CONSTRAINT fk_goods_receipts_approved_by FOREIGN KEY (approved_by) REFERENCES public.users(id);

--
-- Name: goods_receipts fk_goods_receipts_received_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_receipts
    ADD CONSTRAINT fk_goods_receipts_received_by FOREIGN KEY (received_by) REFERENCES public.users(id);

--
-- Name: product_images fk_images_variant; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_images
    ADD CONSTRAINT fk_images_variant FOREIGN KEY (product_variant_id) REFERENCES public.product_variants(id);

--
-- Name: internal_chat_messages fk_internal_chat_messages_room; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.internal_chat_messages
    ADD CONSTRAINT fk_internal_chat_messages_room FOREIGN KEY (room_id) REFERENCES public.internal_chat_rooms(id) ON DELETE CASCADE;

--
-- Name: internal_chat_rooms fk_internal_chat_rooms_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.internal_chat_rooms
    ADD CONSTRAINT fk_internal_chat_rooms_created_by FOREIGN KEY (created_by) REFERENCES public.users(id);

--
-- Name: inventory_balances fk_inventory_balance_store; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_balances
    ADD CONSTRAINT fk_inventory_balance_store FOREIGN KEY (store_id) REFERENCES public.stores(id);

--
-- Name: inventory_balances fk_inventory_balances_variant; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_balances
    ADD CONSTRAINT fk_inventory_balances_variant FOREIGN KEY (product_variant_id) REFERENCES public.product_variants(id);

--
-- Name: inventory_transactions fk_inventory_transaction_store; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_transactions
    ADD CONSTRAINT fk_inventory_transaction_store FOREIGN KEY (store_id) REFERENCES public.stores(id);

--
-- Name: inventory_transactions fk_inventory_transactions_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_transactions
    ADD CONSTRAINT fk_inventory_transactions_created_by FOREIGN KEY (created_by) REFERENCES public.users(id);

--
-- Name: inventory_transactions fk_inventory_transactions_variant; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_transactions
    ADD CONSTRAINT fk_inventory_transactions_variant FOREIGN KEY (product_variant_id) REFERENCES public.product_variants(id);

--
-- Name: loyalty_accounts fk_loyalty_accounts_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_accounts
    ADD CONSTRAINT fk_loyalty_accounts_customer FOREIGN KEY (customer_id) REFERENCES public.customers(id);

--
-- Name: loyalty_accounts fk_loyalty_accounts_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_accounts
    ADD CONSTRAINT fk_loyalty_accounts_user FOREIGN KEY (user_id) REFERENCES public.users(id);

--
-- Name: loyalty_transactions fk_loyalty_transactions_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_transactions
    ADD CONSTRAINT fk_loyalty_transactions_customer FOREIGN KEY (customer_id) REFERENCES public.customers(id);

--
-- Name: loyalty_transactions fk_loyalty_transactions_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_transactions
    ADD CONSTRAINT fk_loyalty_transactions_user FOREIGN KEY (user_id) REFERENCES public.users(id);

--
-- Name: notification_logs fk_notification_logs_notification; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_logs
    ADD CONSTRAINT fk_notification_logs_notification FOREIGN KEY (notification_id) REFERENCES public.notifications(id) ON DELETE CASCADE;

--
-- Name: order_addresses fk_order_addresses_order; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_addresses
    ADD CONSTRAINT fk_order_addresses_order FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;

--
-- Name: order_chat_message_status fk_order_chat_message_status_message; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_chat_message_status
    ADD CONSTRAINT fk_order_chat_message_status_message FOREIGN KEY (message_id) REFERENCES public.order_chat_messages(id) ON DELETE CASCADE;

--
-- Name: order_chat_message_status fk_order_chat_message_status_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_chat_message_status
    ADD CONSTRAINT fk_order_chat_message_status_user FOREIGN KEY (user_id) REFERENCES public.users(id);

--
-- Name: order_chat_messages fk_order_chat_messages_room; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_chat_messages
    ADD CONSTRAINT fk_order_chat_messages_room FOREIGN KEY (room_id) REFERENCES public.order_chat_rooms(id) ON DELETE CASCADE;

--
-- Name: order_chat_rooms fk_order_chat_rooms_closed_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_chat_rooms
    ADD CONSTRAINT fk_order_chat_rooms_closed_by FOREIGN KEY (closed_by) REFERENCES public.users(id);

--
-- Name: order_chat_rooms fk_order_chat_rooms_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_chat_rooms
    ADD CONSTRAINT fk_order_chat_rooms_customer FOREIGN KEY (customer_id) REFERENCES public.customers(id);

--
-- Name: order_chat_rooms fk_order_chat_rooms_order; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_chat_rooms
    ADD CONSTRAINT fk_order_chat_rooms_order FOREIGN KEY (order_id) REFERENCES public.orders(id);

--
-- Name: order_chat_rooms fk_order_chat_rooms_staff; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_chat_rooms
    ADD CONSTRAINT fk_order_chat_rooms_staff FOREIGN KEY (assigned_staff_id) REFERENCES public.users(id);

--
-- Name: order_items fk_order_items_order; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;

--
-- Name: order_items fk_order_items_product; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES public.products(id);

--
-- Name: order_items fk_order_items_promotion; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fk_order_items_promotion FOREIGN KEY (promotion_id) REFERENCES public.promotions(id);

--
-- Name: order_items fk_order_items_variant; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fk_order_items_variant FOREIGN KEY (product_variant_id) REFERENCES public.product_variants(id);

--
-- Name: order_promotions fk_order_promotions_order; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_promotions
    ADD CONSTRAINT fk_order_promotions_order FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;

--
-- Name: order_promotions fk_order_promotions_promotion; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_promotions
    ADD CONSTRAINT fk_order_promotions_promotion FOREIGN KEY (promotion_id) REFERENCES public.promotions(id);

--
-- Name: order_status_histories fk_order_status_histories_order; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_status_histories
    ADD CONSTRAINT fk_order_status_histories_order FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;

--
-- Name: orders fk_orders_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk_orders_created_by FOREIGN KEY (created_by) REFERENCES public.users(id);

--
-- Name: orders fk_orders_handler; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk_orders_handler FOREIGN KEY (handler_user_id) REFERENCES public.users(id);

--
-- Name: orders fk_orders_hidden_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk_orders_hidden_by FOREIGN KEY (hidden_by) REFERENCES public.users(id);

--
-- Name: orders fk_orders_parent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk_orders_parent FOREIGN KEY (parent_order_id) REFERENCES public.orders(id);

--
-- Name: orders fk_orders_restored_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk_orders_restored_by FOREIGN KEY (restored_by) REFERENCES public.users(id);

--
-- Name: orders fk_orders_store; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk_orders_store FOREIGN KEY (store_id) REFERENCES public.stores(id);

--
-- Name: orders fk_orders_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES public.users(id);

--
-- Name: payment_transactions fk_payment_transactions_payment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_transactions
    ADD CONSTRAINT fk_payment_transactions_payment FOREIGN KEY (payment_id) REFERENCES public.payments(id) ON DELETE CASCADE;

--
-- Name: payment_webhook_logs fk_payment_webhook_logs_payment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_webhook_logs
    ADD CONSTRAINT fk_payment_webhook_logs_payment FOREIGN KEY (payment_id) REFERENCES public.payments(id);

--
-- Name: payments fk_payments_order; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES public.orders(id);

--
-- Name: permissions fk_permissions_group; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT fk_permissions_group FOREIGN KEY (group_id) REFERENCES public.permission_groups(id) ON DELETE SET NULL;

--
-- Name: products fk_products_brand; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES public.brands(id);

--
-- Name: products fk_products_category; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES public.categories(id);

--
-- Name: products fk_products_collection; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fk_products_collection FOREIGN KEY (collection_id) REFERENCES public.collections(id);

--
-- Name: promotion_conditions fk_promotion_conditions_promotion; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotion_conditions
    ADD CONSTRAINT fk_promotion_conditions_promotion FOREIGN KEY (promotion_id) REFERENCES public.promotions(id) ON DELETE CASCADE;

--
-- Name: promotion_products fk_promotion_products_product; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotion_products
    ADD CONSTRAINT fk_promotion_products_product FOREIGN KEY (product_id) REFERENCES public.products(id);

--
-- Name: promotion_products fk_promotion_products_promotion; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotion_products
    ADD CONSTRAINT fk_promotion_products_promotion FOREIGN KEY (promotion_id) REFERENCES public.promotions(id) ON DELETE CASCADE;

--
-- Name: promotion_tiers fk_promotion_tiers_promotion; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotion_tiers
    ADD CONSTRAINT fk_promotion_tiers_promotion FOREIGN KEY (promotion_id) REFERENCES public.promotions(id) ON DELETE CASCADE;

--
-- Name: promotion_tiers fk_promotion_tiers_tier; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotion_tiers
    ADD CONSTRAINT fk_promotion_tiers_tier FOREIGN KEY (tier_id) REFERENCES public.customer_tiers(id);

--
-- Name: promotion_usages fk_promotion_usages_order; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotion_usages
    ADD CONSTRAINT fk_promotion_usages_order FOREIGN KEY (order_id) REFERENCES public.orders(id);

--
-- Name: promotion_usages fk_promotion_usages_promotion; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotion_usages
    ADD CONSTRAINT fk_promotion_usages_promotion FOREIGN KEY (promotion_id) REFERENCES public.promotions(id) ON DELETE CASCADE;

--
-- Name: promotion_usages fk_promotion_usages_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotion_usages
    ADD CONSTRAINT fk_promotion_usages_user FOREIGN KEY (user_id) REFERENCES public.users(id);

--
-- Name: refunds fk_refunds_order; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refunds
    ADD CONSTRAINT fk_refunds_order FOREIGN KEY (order_id) REFERENCES public.orders(id);

--
-- Name: refunds fk_refunds_payment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refunds
    ADD CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES public.payments(id) ON DELETE CASCADE;

--
-- Name: refunds fk_refunds_requested_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refunds
    ADD CONSTRAINT fk_refunds_requested_by FOREIGN KEY (requested_by) REFERENCES public.users(id);

--
-- Name: role_permissions fk_role_permissions_permission; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES public.permissions(id) ON DELETE CASCADE;

--
-- Name: role_permissions fk_role_permissions_role; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE CASCADE;

--
-- Name: shipments fk_shipments_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipments
    ADD CONSTRAINT fk_shipments_created_by FOREIGN KEY (created_by) REFERENCES public.users(id);

--
-- Name: shipments fk_shipments_order; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipments
    ADD CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;

--
-- Name: shipments fk_shipments_replaced_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipments
    ADD CONSTRAINT fk_shipments_replaced_by FOREIGN KEY (replaced_by_shipment_id) REFERENCES public.shipments(id);

--
-- Name: stock_reservations fk_stock_reservation_store; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_reservations
    ADD CONSTRAINT fk_stock_reservation_store FOREIGN KEY (store_id) REFERENCES public.stores(id);

--
-- Name: stock_reservations fk_stock_reservations_order; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_reservations
    ADD CONSTRAINT fk_stock_reservations_order FOREIGN KEY (order_id) REFERENCES public.orders(id);

--
-- Name: stock_reservations fk_stock_reservations_variant; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_reservations
    ADD CONSTRAINT fk_stock_reservations_variant FOREIGN KEY (product_variant_id) REFERENCES public.product_variants(id);

--
-- Name: store_staffs fk_store_staff_store; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.store_staffs
    ADD CONSTRAINT fk_store_staff_store FOREIGN KEY (store_id) REFERENCES public.stores(id);

--
-- Name: store_staffs fk_store_staffs_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.store_staffs
    ADD CONSTRAINT fk_store_staffs_user FOREIGN KEY (user_id) REFERENCES public.users(id);

--
-- Name: product_tag_mappings fk_tagmap_product; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_tag_mappings
    ADD CONSTRAINT fk_tagmap_product FOREIGN KEY (product_id) REFERENCES public.products(id);

--
-- Name: product_tag_mappings fk_tagmap_tag; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_tag_mappings
    ADD CONSTRAINT fk_tagmap_tag FOREIGN KEY (tag_id) REFERENCES public.product_tags(id);

--
-- Name: user_notification_preferences fk_user_notification_preferences_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_notification_preferences
    ADD CONSTRAINT fk_user_notification_preferences_user FOREIGN KEY (user_id) REFERENCES public.users(id);

--
-- Name: user_permissions fk_user_permissions_permission; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_permissions
    ADD CONSTRAINT fk_user_permissions_permission FOREIGN KEY (permission_id) REFERENCES public.permissions(id) ON DELETE CASCADE;

--
-- Name: user_permissions fk_user_permissions_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_permissions
    ADD CONSTRAINT fk_user_permissions_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: user_roles fk_user_roles_role; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE CASCADE;

--
-- Name: user_roles fk_user_roles_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: user_tiers fk_user_tiers_tier; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_tiers
    ADD CONSTRAINT fk_user_tiers_tier FOREIGN KEY (tier_id) REFERENCES public.customer_tiers(id);

--
-- Name: user_tiers fk_user_tiers_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_tiers
    ADD CONSTRAINT fk_user_tiers_user FOREIGN KEY (user_id) REFERENCES public.users(id);

--
-- Name: user_tokens fk_user_tokens_parent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_tokens
    ADD CONSTRAINT fk_user_tokens_parent FOREIGN KEY (parent_token_id) REFERENCES public.user_tokens(id) ON DELETE SET NULL;

--
-- Name: user_tokens fk_user_tokens_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_tokens
    ADD CONSTRAINT fk_user_tokens_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: product_variants fk_variants_product; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_variants
    ADD CONSTRAINT fk_variants_product FOREIGN KEY (product_id) REFERENCES public.products(id);

--
-- Name: inventory_balances fk_warehouse_balance_variant; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_balances
    ADD CONSTRAINT fk_warehouse_balance_variant FOREIGN KEY (product_variant_id) REFERENCES public.product_variants(id);

--
-- Name: goods_issues fk_warehouse_issue_completed_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_issues
    ADD CONSTRAINT fk_warehouse_issue_completed_by FOREIGN KEY (completed_by) REFERENCES public.users(id);

--
-- Name: goods_issue_items fk_warehouse_issue_item_product; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_issue_items
    ADD CONSTRAINT fk_warehouse_issue_item_product FOREIGN KEY (product_id) REFERENCES public.products(id);

--
-- Name: goods_issue_items fk_warehouse_issue_item_variant_product; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_issue_items
    ADD CONSTRAINT fk_warehouse_issue_item_variant_product FOREIGN KEY (product_variant_id, product_id) REFERENCES public.product_variants(id, product_id);

--
-- Name: goods_issues fk_warehouse_issue_supplier; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_issues
    ADD CONSTRAINT fk_warehouse_issue_supplier FOREIGN KEY (supplier_id) REFERENCES public.suppliers(id);

--
-- Name: goods_receipts fk_warehouse_receipt_completed_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_receipts
    ADD CONSTRAINT fk_warehouse_receipt_completed_by FOREIGN KEY (completed_by) REFERENCES public.users(id);

--
-- Name: goods_receipt_items fk_warehouse_receipt_item_product; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_receipt_items
    ADD CONSTRAINT fk_warehouse_receipt_item_product FOREIGN KEY (product_id) REFERENCES public.products(id);

--
-- Name: goods_receipt_items fk_warehouse_receipt_item_variant_product; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_receipt_items
    ADD CONSTRAINT fk_warehouse_receipt_item_variant_product FOREIGN KEY (product_variant_id, product_id) REFERENCES public.product_variants(id, product_id);

--
-- Name: inventory_transactions fk_warehouse_transaction_export; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_transactions
    ADD CONSTRAINT fk_warehouse_transaction_export FOREIGN KEY (export_receipt_id) REFERENCES public.goods_issues(id);

--
-- Name: inventory_transactions fk_warehouse_transaction_import; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_transactions
    ADD CONSTRAINT fk_warehouse_transaction_import FOREIGN KEY (import_receipt_id) REFERENCES public.goods_receipts(id);

--
-- Name: inventory_transactions fk_warehouse_transaction_variant; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_transactions
    ADD CONSTRAINT fk_warehouse_transaction_variant FOREIGN KEY (product_variant_id) REFERENCES public.product_variants(id);

--
-- Name: permission_groups fkpcdag4vxfph8bqc03cr2h8g6v; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permission_groups
    ADD CONSTRAINT fkpcdag4vxfph8bqc03cr2h8g6v FOREIGN KEY (module_id) REFERENCES public.modules(id);

--
-- Name: order_customer_contacts order_customer_contacts_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_customer_contacts
    ADD CONSTRAINT order_customer_contacts_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id);

--
-- Name: order_customer_contacts order_customer_contacts_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_customer_contacts
    ADD CONSTRAINT order_customer_contacts_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);

--
-- Name: order_customer_contacts order_customer_contacts_sent_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_customer_contacts
    ADD CONSTRAINT order_customer_contacts_sent_by_fkey FOREIGN KEY (sent_by) REFERENCES public.users(id);

--
-- Name: support_faqs support_faqs_updated_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.support_faqs
    ADD CONSTRAINT support_faqs_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users(id);

--
-- Name: wishlist_items wishlist_items_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wishlist_items
    ADD CONSTRAINT wishlist_items_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id) ON DELETE CASCADE;

--
-- Name: wishlist_items wishlist_items_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wishlist_items
    ADD CONSTRAINT wishlist_items_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id) ON DELETE CASCADE;
