import React, { useState, useEffect } from 'react';
import { MedicalReviewData, DiagnosisDiseaseModel } from '../types';
import Loading from './Loading';

interface PatientDiagnosisTabProps {
  data: MedicalReviewData;
  onDataChange: (data: MedicalReviewData) => void;
}

const PatientDiagnosisTab: React.FC<PatientDiagnosisTabProps> = ({ data, onDataChange }) => {
  const [diagnoses, setDiagnoses] = useState<DiagnosisDiseaseModel[]>([]);
  const [loading, setLoading] = useState(false);
  const [showAddDiagnosis, setShowAddDiagnosis] = useState(false);
  const [selectedDiagnosis, setSelectedDiagnosis] = useState('');

  const mockDiagnoses = [
    { id: 1, diseaseCategory: 'diabetes', name: 'Type 2 Diabetes', value: 'diabetes' },
    { id: 2, diseaseCategory: 'hypertension', name: 'Essential Hypertension', value: 'hypertension' },
    { id: 3, diseaseCategory: 'cardiovascular', name: 'Coronary Artery Disease', value: 'cad' },
    { id: 4, diseaseCategory: 'respiratory', name: 'Chronic Obstructive Pulmonary Disease', value: 'copd' },
    { id: 5, diseaseCategory: 'metabolic', name: 'Metabolic Syndrome', value: 'metabolic_syndrome' }
  ];

  useEffect(() => {
    if (data.diagnoses) {
      setDiagnoses(data.diagnoses);
    }
  }, [data.diagnoses]);

  const handleAddDiagnosis = () => {
    if (selectedDiagnosis) {
      const diagnosis = mockDiagnoses.find(d => d.value === selectedDiagnosis);
      if (diagnosis && !diagnoses.find(d => d.id === diagnosis.id)) {
        const updatedDiagnoses = [...diagnoses, diagnosis];
        setDiagnoses(updatedDiagnoses);
        onDataChange({
          ...data,
          diagnoses: updatedDiagnoses
        });
        setSelectedDiagnosis('');
        setShowAddDiagnosis(false);
      }
    }
  };

  const handleRemoveDiagnosis = (diagnosisId: number) => {
    const updatedDiagnoses = diagnoses.filter(d => d.id !== diagnosisId);
    setDiagnoses(updatedDiagnoses);
    onDataChange({
      ...data,
      diagnoses: updatedDiagnoses
    });
  };

  const handleWeightBpAction = (action: string) => {
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      alert(`${action} recorded successfully`);
    }, 1000);
  };

  if (loading) {
    return <Loading />;
  }

  return (
    <div>
      <div className="card">
        <h3 style={{ marginBottom: '16px', color: '#333' }}>Vital Signs</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '16px' }}>
          <div style={{ 
            border: '1px solid #ddd', 
            borderRadius: '8px', 
            padding: '16px',
            background: '#f8f9fa'
          }}>
            <h4 style={{ marginBottom: '8px', color: '#1976d2' }}>Weight</h4>
            <p style={{ fontSize: '18px', fontWeight: 'bold', marginBottom: '8px' }}>
              {data.patientDetails?.id ? '65.5 kg' : '-- kg'}
            </p>
            <button 
              className="btn btn-primary"
              style={{ fontSize: '14px', padding: '8px 16px' }}
              onClick={() => handleWeightBpAction('Weight')}
            >
              Add Weight
            </button>
          </div>
          
          <div style={{ 
            border: '1px solid #ddd', 
            borderRadius: '8px', 
            padding: '16px',
            background: '#f8f9fa'
          }}>
            <h4 style={{ marginBottom: '8px', color: '#1976d2' }}>Blood Pressure</h4>
            <p style={{ fontSize: '18px', fontWeight: 'bold', marginBottom: '8px' }}>
              {data.patientDetails?.id ? '120/80 mmHg' : '-- / -- mmHg'}
            </p>
            <button 
              className="btn btn-primary"
              style={{ fontSize: '14px', padding: '8px 16px' }}
              onClick={() => handleWeightBpAction('Blood Pressure')}
            >
              Add BP
            </button>
          </div>
        </div>
      </div>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3 style={{ color: '#333', margin: 0 }}>Diagnoses</h3>
          <button 
            className="btn btn-primary"
            onClick={() => setShowAddDiagnosis(!showAddDiagnosis)}
          >
            {diagnoses.length > 0 ? 'Edit Diagnoses' : 'Add Diagnosis'}
          </button>
        </div>

        {diagnoses.length > 0 ? (
          <div>
            {diagnoses.map((diagnosis) => (
              <div key={diagnosis.id} style={{ 
                display: 'flex', 
                justifyContent: 'space-between', 
                alignItems: 'center',
                padding: '12px',
                border: '1px solid #ddd',
                borderRadius: '4px',
                marginBottom: '8px',
                background: '#f8f9fa'
              }}>
                <div>
                  <strong>{diagnosis.name}</strong>
                  <p style={{ margin: '4px 0 0 0', color: '#666', fontSize: '14px' }}>
                    Category: {diagnosis.diseaseCategory}
                  </p>
                </div>
                <button 
                  onClick={() => handleRemoveDiagnosis(diagnosis.id)}
                  style={{ 
                    background: '#d32f2f', 
                    color: 'white', 
                    border: 'none', 
                    padding: '6px 12px', 
                    borderRadius: '4px',
                    cursor: 'pointer'
                  }}
                >
                  Remove
                </button>
              </div>
            ))}
          </div>
        ) : (
          <p style={{ color: '#666', fontStyle: 'italic' }}>No diagnoses added yet</p>
        )}

        {showAddDiagnosis && (
          <div style={{ marginTop: '16px', padding: '16px', border: '1px solid #ddd', borderRadius: '8px', background: '#f8f9fa' }}>
            <h4 style={{ marginBottom: '12px' }}>Add New Diagnosis</h4>
            <div className="form-group">
              <label className="form-label">Select Diagnosis</label>
              <select 
                className="form-input"
                value={selectedDiagnosis}
                onChange={(e) => setSelectedDiagnosis(e.target.value)}
              >
                <option value="">Choose a diagnosis...</option>
                {mockDiagnoses
                  .filter(d => !diagnoses.find(existing => existing.id === d.id))
                  .map((diagnosis) => (
                    <option key={diagnosis.id} value={diagnosis.value}>
                      {diagnosis.name}
                    </option>
                  ))
                }
              </select>
            </div>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button 
                className="btn btn-primary"
                onClick={handleAddDiagnosis}
                disabled={!selectedDiagnosis}
              >
                Add Diagnosis
              </button>
              <button 
                className="btn"
                style={{ background: '#f5f5f5', color: '#333' }}
                onClick={() => setShowAddDiagnosis(false)}
              >
                Cancel
              </button>
            </div>
          </div>
        )}
      </div>

      <div className="card">
        <h3 style={{ marginBottom: '16px', color: '#333' }}>Patient Status</h3>
        <p style={{ fontSize: '18px', fontWeight: 'bold', color: '#1976d2' }}>
          {data.patientStatus?.status || 'Active'}
        </p>
      </div>
    </div>
  );
};

export default PatientDiagnosisTab;
