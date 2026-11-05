import React, { useState, useEffect } from 'react';
import './RequestApprovalManagement.css';

const RequestApprovalManagement = () => {
  const [tab, setTab] = useState('excuses');
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState('PENDING');
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [showActionModal, setShowActionModal] = useState(false);
  const [actionType, setActionType] = useState('approve');
  const [actionData, setActionData] = useState({ reason: '' });

  useEffect(() => {
    fetchRequests();
  }, [tab, status]);

  const fetchRequests = async () => {
    setLoading(true);
    try {
      const endpoint = tab === 'excuses' ? '/api/admin/attendance/excuses' : '/api/admin/attendance/leave';
      const params = new URLSearchParams({ status });
      const response = await fetch(`${endpoint}?${params}`, {
        credentials: 'include'
      });
      if (response.ok) {
        const data = await response.json();
        setRequests(data.requests || []);
      }
    } catch (err) {
      console.error('Error fetching requests:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleAction = (request, type) => {
    setSelectedRequest(request);
    setActionType(type);
    setActionData({ reason: '' });
    setShowActionModal(true);
  };

  const submitAction = async () => {
    const endpoint = tab === 'excuses'
      ? `/api/admin/attendance/excuses/${selectedRequest.id}/${actionType}`
      : `/api/admin/attendance/leave/${selectedRequest.id}/${actionType}`;

    const payload = actionType === 'approve'
      ? { proposed_check_in: '', proposed_check_out: '' }
      : { reason: actionData.reason };

    try {
      const response = await fetch(endpoint, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(payload)
      });

      if (response.ok) {
        alert(`${actionType === 'approve' ? 'Duyệt' : 'Từ chối'} thành công`);
        setShowActionModal(false);
        fetchRequests();
      }
    } catch (err) {
      alert('Lỗi: ' + err.message);
    }
  };

  const formatDate = (dateStr) => {
    return new Date(dateStr).toLocaleDateString('vi-VN');
  };

  return (
    <div className="request-management">
      <h2>Quản lý phê duyệt</h2>

      <div className="tabs">
        <button
          className={`tab ${tab === 'excuses' ? 'active' : ''}`}
          onClick={() => setTab('excuses')}
        >
          Đơn giải trình
        </button>
        <button
          className={`tab ${tab === 'leave' ? 'active' : ''}`}
          onClick={() => setTab('leave')}
        >
          Đơn xin nghỉ
        </button>
      </div>

      <div className="filter-section">
        <div className="filter-group">
          <label>Trạng thái:</label>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="PENDING">Chờ duyệt</option>
            <option value="APPROVED">Đã duyệt</option>
            <option value="REJECTED">Bị từ chối</option>
          </select>
        </div>
      </div>

      <div className="requests-table">
        {loading ? (
          <p className="loading">Đang tải...</p>
        ) : requests.length > 0 ? (
          <table>
            <thead>
              <tr>
                <th>Nhân viên</th>
                <th>Ngày</th>
                {tab === 'leave' && <th>Loại</th>}
                <th>Lý do</th>
                <th>Trạng thái</th>
                {status === 'PENDING' && <th>Hành động</th>}
              </tr>
            </thead>
            <tbody>
              {requests.map((request) => (
                <tr key={request.id}>
                  <td>{request.employee_name || '-'}</td>
                  <td>
                    {tab === 'excuses' ? formatDate(request.attendance_date) :
                     `${formatDate(request.start_date)} - ${formatDate(request.end_date)}`}
                  </td>
                  {tab === 'leave' && <td>{request.leave_type_name}</td>}
                  <td className="reason">{request.reason}</td>
                  <td>
                    <span className={`status status-${request.status.toLowerCase()}`}>
                      {request.status}
                    </span>
                  </td>
                  {status === 'PENDING' && (
                    <td>
                      <div className="action-buttons-cell">
                        <button
                          onClick={() => handleAction(request, 'approve')}
                          className="btn-small btn-approve"
                        >
                          Duyệt
                        </button>
                        <button
                          onClick={() => handleAction(request, 'reject')}
                          className="btn-small btn-reject"
                        >
                          Từ chối
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="no-data">Không có dữ liệu</p>
        )}
      </div>

      {showActionModal && (
        <div className="modal-overlay">
          <div className="modal">
            <h3>{actionType === 'approve' ? 'Phê duyệt' : 'Từ chối'}</h3>
            {actionType === 'reject' && (
              <div className="form-group">
                <label>Lý do từ chối:</label>
                <textarea
                  value={actionData.reason}
                  onChange={(e) => setActionData({ ...actionData, reason: e.target.value })}
                  rows="4"
                  placeholder="Nhập lý do từ chối..."
                />
              </div>
            )}
            <div className="modal-buttons">
              <button onClick={submitAction} className="btn-primary">
                {actionType === 'approve' ? 'Phê duyệt' : 'Từ chối'}
              </button>
              <button onClick={() => setShowActionModal(false)} className="btn-cancel">
                Hủy
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default RequestApprovalManagement;
