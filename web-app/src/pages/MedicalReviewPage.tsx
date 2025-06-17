import React, { useState, useEffect } from 'react';
import Header from '../components/Header';
import PatientExaminationTab from '../components/PatientExaminationTab';
import PatientDiagnosisTab from '../components/PatientDiagnosisTab';
import PatientSummaryTab from '../components/PatientSummaryTab';
import { MedicalReviewData, PatientDetailModel } from '../types';

const MedicalReviewPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState('examination');
  const [medicalReviewData, setMedicalReviewData] = useState<MedicalReviewData>({});

  const mockPatient: PatientDetailModel = {
    id: '1',
    patientId: 'PAT001',
    firstName: 'John',
    lastName: 'Doe',
    age: 45,
    gender: 'Male',
    phoneNumber: '+1234567890',
    nationalId: 'ID123456789',
    programId: 'NCD001'
  };

  useEffect(() => {
    setMedicalReviewData({
      patientDetails: mockPatient,
      patientStatus: { status: 'Active' },
      diagnoses: [],
      complaints: [],
      examinations: []
    });
  }, []);

  const tabs = [
    { id: 'examination', label: 'Patient Examination' },
    { id: 'diagnosis', label: 'Diagnosis' },
    { id: 'summary', label: 'Summary' }
  ];

  const renderTabContent = () => {
    switch (activeTab) {
      case 'examination':
        return <PatientExaminationTab data={medicalReviewData} onDataChange={setMedicalReviewData} />;
      case 'diagnosis':
        return <PatientDiagnosisTab data={medicalReviewData} onDataChange={setMedicalReviewData} />;
      case 'summary':
        return <PatientSummaryTab data={medicalReviewData} />;
      default:
        return <PatientExaminationTab data={medicalReviewData} onDataChange={setMedicalReviewData} />;
    }
  };

  return (
    <div>
      <Header />
      <div className="container">
        <div className="card">
          <h2 style={{ marginBottom: '16px', color: '#333' }}>Patient Medical Review</h2>
          
          {medicalReviewData.patientDetails && (
            <div style={{ 
              background: '#f8f9fa', 
              padding: '16px', 
              borderRadius: '8px', 
              marginBottom: '24px' 
            }}>
              <h3 style={{ marginBottom: '8px', color: '#1976d2' }}>
                {medicalReviewData.patientDetails.firstName} {medicalReviewData.patientDetails.lastName}
              </h3>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '8px' }}>
                <p><strong>Patient ID:</strong> {medicalReviewData.patientDetails.patientId}</p>
                <p><strong>Age:</strong> {medicalReviewData.patientDetails.age}</p>
                <p><strong>Gender:</strong> {medicalReviewData.patientDetails.gender}</p>
                <p><strong>Phone:</strong> {medicalReviewData.patientDetails.phoneNumber}</p>
                <p><strong>National ID:</strong> {medicalReviewData.patientDetails.nationalId}</p>
                <p><strong>Status:</strong> {medicalReviewData.patientStatus?.status || 'Unknown'}</p>
              </div>
            </div>
          )}

          <div className="tabs">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                className={`tab ${activeTab === tab.id ? 'active' : ''}`}
                onClick={() => setActiveTab(tab.id)}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {renderTabContent()}
        </div>
      </div>
    </div>
  );
};

export default MedicalReviewPage;
