import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import AttendanceDashboard from './AttendanceDashboard';
import AttendanceRecordsManagement from './AttendanceRecordsManagement';
import RequestApprovalManagement from './RequestApprovalManagement';
import AttendanceExportReports from './AttendanceExportReports';

const AttendanceLayout = () => {
  return (
    <Routes>
      <Route index element={<AttendanceDashboard />} />
      <Route path="records" element={<AttendanceRecordsManagement />} />
      <Route path="excuses" element={<RequestApprovalManagement />} />
      <Route path="leave" element={<RequestApprovalManagement />} />
      <Route path="export" element={<AttendanceExportReports />} />
      <Route path="*" element={<Navigate to="." />} />
    </Routes>
  );
};

export default AttendanceLayout;
