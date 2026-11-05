import React, { useState, useEffect } from 'react';
import './CheckInOut.css';

const CheckInOut = () => {
  const [currentTime, setCurrentTime] = useState(new Date());
  const [todayStatus, setTodayStatus] = useState(null);
  const [loading, setLoading] = useState(false);
  const [location, setLocation] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    fetchTodayStatus();
  }, []);

  const fetchTodayStatus = async () => {
    try {
      const response = await fetch('/api/attendance/today', {
        credentials: 'include'
      });
      if (response.ok) {
        const data = await response.json();
        setTodayStatus(data);
      }
    } catch (err) {
      console.error('Error fetching today status:', err);
    }
  };

  const getLocation = () => {
    return new Promise((resolve, reject) => {
      if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
          (position) => {
            resolve({
              latitude: position.coords.latitude,
              longitude: position.coords.longitude
            });
          },
          (error) => {
            console.warn('Geolocation denied or unavailable:', error);
            resolve({
              latitude: null,
              longitude: null
            });
          }
        );
      } else {
        resolve({
          latitude: null,
          longitude: null
        });
      }
    });
  };

  const handleCheckIn = async () => {
    setLoading(true);
    setError(null);
    try {
      const loc = await getLocation();
      const response = await fetch('/api/attendance/check-in', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({
          latitude: loc.latitude,
          longitude: loc.longitude
        })
      });

      if (response.ok) {
        fetchTodayStatus();
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Check-in failed');
      }
    } catch (err) {
      setError('Error during check-in: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCheckOut = async () => {
    setLoading(true);
    setError(null);
    try {
      const loc = await getLocation();
      const response = await fetch('/api/attendance/check-out', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({
          latitude: loc.latitude,
          longitude: loc.longitude
        })
      });

      if (response.ok) {
        fetchTodayStatus();
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Check-out failed');
      }
    } catch (err) {
      setError('Error during check-out: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const formatTime = (date) => {
    return date.toLocaleTimeString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  const formatDateTime = (dateTimeStr) => {
    if (!dateTimeStr) return '-';
    return new Date(dateTimeStr).toLocaleString('vi-VN');
  };

  const canCheckIn = !todayStatus?.checked_in || todayStatus?.checked_in === false;
  const canCheckOut = todayStatus?.checked_in && !todayStatus?.checked_out;

  return (
    <div className="check-in-out">
      <div className="current-time">
        <h2>Giờ hiện tại</h2>
        <div className="time-display">{formatTime(currentTime)}</div>
        <div className="date-display">{currentTime.toLocaleDateString('vi-VN')}</div>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="status-card">
        {todayStatus && (
          <>
            <div className="status-item">
              <span className="label">Check-in:</span>
              <span className="value">
                {formatDateTime(todayStatus.check_in_time)}
              </span>
            </div>
            <div className="status-item">
              <span className="label">Check-out:</span>
              <span className="value">
                {formatDateTime(todayStatus.check_out_time)}
              </span>
            </div>
            {todayStatus.duration_minutes && (
              <div className="status-item">
                <span className="label">Tổng thời gian:</span>
                <span className="value">
                  {Math.floor(todayStatus.duration_minutes / 60)}h {todayStatus.duration_minutes % 60}m
                </span>
              </div>
            )}
            <div className="status-item">
              <span className="label">Trạng thái:</span>
              <span className="value">{todayStatus.status}</span>
            </div>
          </>
        )}
      </div>

      <div className="button-group">
        <button
          onClick={handleCheckIn}
          disabled={!canCheckIn || loading}
          className="btn btn-check-in"
        >
          {loading ? 'Đang xử lý...' : 'Check-in'}
        </button>
        <button
          onClick={handleCheckOut}
          disabled={!canCheckOut || loading}
          className="btn btn-check-out"
        >
          {loading ? 'Đang xử lý...' : 'Check-out'}
        </button>
      </div>
    </div>
  );
};

export default CheckInOut;
