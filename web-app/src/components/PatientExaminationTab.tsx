import React, { useState, useEffect } from 'react';
import ChipSelector from './ChipSelector';
import { MedicalReviewData, ChipViewItemModel } from '../types';

interface PatientExaminationTabProps {
  data: MedicalReviewData;
  onDataChange: (data: MedicalReviewData) => void;
}

const PatientExaminationTab: React.FC<PatientExaminationTabProps> = ({ data, onDataChange }) => {
  const [complaints, setComplaints] = useState<ChipViewItemModel[]>([]);
  const [examinations, setExaminations] = useState<ChipViewItemModel[]>([]);

  useEffect(() => {
    const complaintsList = [
      "Headache", "Abdominal Pain", "Difficulty in breathing", "Chest Pain", 
      "Body Swelling", "Joint/Back Ache", "Injury/cut/bruise", 
      "Eye pain/discharge/itchiness/difficulty seeing", "Skin rash/itchiness", 
      "Burns", "Genital pain/swelling/discharge/bleeding", "Pain on passing urine", 
      "Toothache/gum swelling", "None", "Other"
    ].map(item => ({
      name: item,
      cultureValue: item,
      type: "Complaints",
      selected: false
    }));

    const examsList = [
      "Eye Exam", "Oral Exam", "Cardiovascular", "Respiratory system", 
      "Abdominal Pelvic", "Foot Exam", "Neurological Exam"
    ].map(item => ({
      name: item,
      cultureValue: item,
      type: "Examinations",
      selected: false
    }));

    setComplaints(complaintsList);
    setExaminations(examsList);
  }, []);

  const handleComplaintsChange = (selectedComplaints: ChipViewItemModel[]) => {
    const updatedComplaints = complaints.map(complaint => ({
      ...complaint,
      selected: selectedComplaints.some(selected => selected.name === complaint.name)
    }));
    setComplaints(updatedComplaints);
    
    onDataChange({
      ...data,
      complaints: selectedComplaints
    });
  };

  const handleExaminationsChange = (selectedExaminations: ChipViewItemModel[]) => {
    const updatedExaminations = examinations.map(exam => ({
      ...exam,
      selected: selectedExaminations.some(selected => selected.name === exam.name)
    }));
    setExaminations(updatedExaminations);
    
    onDataChange({
      ...data,
      examinations: selectedExaminations
    });
  };

  return (
    <div>
      <ChipSelector
        items={complaints}
        onSelectionChange={handleComplaintsChange}
        title="Presenting Complaints"
        multiSelect={true}
      />
      
      <ChipSelector
        items={examinations}
        onSelectionChange={handleExaminationsChange}
        title="Physical Examinations"
        multiSelect={true}
      />

      <div className="card">
        <h3 style={{ marginBottom: '16px', color: '#333' }}>Clinical Notes</h3>
        <textarea
          className="form-input"
          rows={4}
          placeholder="Enter clinical notes and observations..."
          style={{ resize: 'vertical', minHeight: '100px' }}
        />
      </div>
    </div>
  );
};

export default PatientExaminationTab;
