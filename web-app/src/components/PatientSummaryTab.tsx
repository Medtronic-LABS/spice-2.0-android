import React from 'react';
import { MedicalReviewData } from '../types';

interface PatientSummaryTabProps {
  data: MedicalReviewData;
}

const PatientSummaryTab: React.FC<PatientSummaryTabProps> = ({ data }) => {
  const handleSubmitReview = () => {
    alert('Medical review submitted successfully!');
  };

  return (
    <div>
      <div className="card">
        <h3 style={{ marginBottom: '16px', color: '#333' }}>Medical Review Summary</h3>
        
        <div style={{ marginBottom: '24px' }}>
          <h4 style={{ color: '#1976d2', marginBottom: '8px' }}>Patient Information</h4>
          {data.patientDetails ? (
            <div style={{ background: '#f8f9fa', padding: '12px', borderRadius: '4px' }}>
              <p><strong>Name:</strong> {data.patientDetails.firstName} {data.patientDetails.lastName}</p>
              <p><strong>Age:</strong> {data.patientDetails.age}</p>
              <p><strong>Gender:</strong> {data.patientDetails.gender}</p>
              <p><strong>Patient ID:</strong> {data.patientDetails.patientId}</p>
            </div>
          ) : (
            <p style={{ color: '#666', fontStyle: 'italic' }}>No patient information available</p>
          )}
        </div>

        <div style={{ marginBottom: '24px' }}>
          <h4 style={{ color: '#1976d2', marginBottom: '8px' }}>Presenting Complaints</h4>
          {data.complaints && data.complaints.length > 0 ? (
            <div style={{ background: '#f8f9fa', padding: '12px', borderRadius: '4px' }}>
              {data.complaints.map((complaint, index) => (
                <span key={index} className="chip" style={{ margin: '2px' }}>
                  {complaint.cultureValue}
                </span>
              ))}
            </div>
          ) : (
            <p style={{ color: '#666', fontStyle: 'italic' }}>No complaints recorded</p>
          )}
        </div>

        <div style={{ marginBottom: '24px' }}>
          <h4 style={{ color: '#1976d2', marginBottom: '8px' }}>Physical Examinations</h4>
          {data.examinations && data.examinations.length > 0 ? (
            <div style={{ background: '#f8f9fa', padding: '12px', borderRadius: '4px' }}>
              {data.examinations.map((exam, index) => (
                <span key={index} className="chip" style={{ margin: '2px' }}>
                  {exam.cultureValue}
                </span>
              ))}
            </div>
          ) : (
            <p style={{ color: '#666', fontStyle: 'italic' }}>No examinations recorded</p>
          )}
        </div>

        <div style={{ marginBottom: '24px' }}>
          <h4 style={{ color: '#1976d2', marginBottom: '8px' }}>Diagnoses</h4>
          {data.diagnoses && data.diagnoses.length > 0 ? (
            <div style={{ background: '#f8f9fa', padding: '12px', borderRadius: '4px' }}>
              {data.diagnoses.map((diagnosis, index) => (
                <div key={index} style={{ marginBottom: '8px' }}>
                  <strong>{diagnosis.name}</strong>
                  <p style={{ margin: '4px 0 0 0', color: '#666', fontSize: '14px' }}>
                    Category: {diagnosis.diseaseCategory}
                  </p>
                </div>
              ))}
            </div>
          ) : (
            <p style={{ color: '#666', fontStyle: 'italic' }}>No diagnoses recorded</p>
          )}
        </div>

        <div style={{ marginBottom: '24px' }}>
          <h4 style={{ color: '#1976d2', marginBottom: '8px' }}>Patient Status</h4>
          <p style={{ fontSize: '16px', fontWeight: 'bold', color: '#1976d2' }}>
            {data.patientStatus?.status || 'Active'}
          </p>
        </div>

        <div style={{ textAlign: 'center' }}>
          <button 
            className="btn btn-primary"
            onClick={handleSubmitReview}
            style={{ fontSize: '16px', padding: '12px 32px' }}
          >
            Submit Medical Review
          </button>
        </div>
      </div>
    </div>
  );
};

export default PatientSummaryTab;
