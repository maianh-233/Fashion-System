import React, { useState, useEffect } from 'react';
import './AttendanceHistory.css';

const AttendanceHistory = () => {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(false);
  const [fromDate, setFromDate] = useState(() => {
    const date = new Date();
    date.setDate(date.getDate() - 30);
    return date.toISOString().split('T')[0];
  });
  const [toDate, setToDate] = useState(new Date().toISOString().split('T')[0]);

  useEffect(() => {
    fetchHistory();
  }, []);

  const fetchHistory = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({
        fromDate,
        toDate
      });
      const response = await fetch(`/api/attendance/history?${params}`, {
        credentials: 'include'
      });
      if (response.ok) {
        const data = await response.json();
        setRecords(data.records || []);
      }
    } catch (err) {
      console.error('Error fetching attendance history:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    fetchHistory();
  };

  const formatDateTime = (dateTimeStr) => {
    if (!dateTimeStr) return '-';
    return new Date(dateTimeStr).toLocaleString('vi-VN');
  };

  const calculateDuration = (checkIn, checkOut) => {
    if (!checkIn || !checkOut) return '-';
    const start = new Date(checkIn);
    const end = new Date(checkOut);
    const minutes = Math.floor((end - start) / (1000 * 60));
    return `${Math.floor(minutes / 60)}h ${minutes % 60}m`;
  };

  return (
    <div className="attendance-history">
      <h2>Lịch sử chấm công</h2>

      <div className="filter-section">
        <div className="filter-group">
          <label>Từ ngày:</label>
          <input
            type="date"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label>Đến ngày:</label>
          <input
            type="date"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
          />
        </div>
        <button onClick={handleSearch} className="btn-search" disabled={loading}>
          {loading ? 'Đang tìm...' : 'Tìm kiếm'}
        </button>
      </div>

      <div className="records-table">
        {records.length > 0 ? (
          <table>
            <thead>
              <tr>
                <th>Ngày</th>
                <th>Check-in</th>
                <th>Check-out</th>
                <th>Thời gian làm</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {records.map((record) => (
                <tr key={record.id}>
                  <td>{record.attendance_date}</td>
                  <td className="time">{formatDateTime(record.check_in_time)}</td>
                  <td className="time">{formatDateTime(record.check_out_time)}</td>
                  <td>{calculateDuration(record.check_in_time, record.check_out_time)}</td>
                  <td>
                    <span className={`status status-${record.status.toLowerCase()}`}>
                      {record.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="no-data">Không có dữ liệu</p>
        )}
      </div>
    </div>
  );
};

export default AttendanceHistory;
