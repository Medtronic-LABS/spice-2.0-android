import axios, { AxiosInstance, AxiosResponse } from 'axios';
import { LoginResponse, LoginRequest, APIResponse, PatientStatusResponse, DiagnosisDiseaseModel } from '../types';

class ApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = axios.create({
      baseURL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.api.interceptors.request.use((config) => {
      const token = localStorage.getItem('authToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });

    this.api.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          localStorage.removeItem('authToken');
          localStorage.removeItem('user');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const formData = new FormData();
    formData.append('username', credentials.username);
    formData.append('password', credentials.password);

    const response: AxiosResponse<LoginResponse> = await this.api.post('/auth-service/session', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    if (response.data.authorization) {
      localStorage.setItem('authToken', response.data.authorization);
      localStorage.setItem('user', JSON.stringify(response.data));
    }

    return response.data;
  }

  async getPatientStatus(patientReference: string): Promise<PatientStatusResponse> {
    const response: AxiosResponse<APIResponse<PatientStatusResponse>> = await this.api.post(
      '/spice-service/patient/patient-status',
      { patientReference }
    );
    return response.data.entity || response.data.data || {};
  }

  async getDiagnosisDetails(patientReference: string, type: string): Promise<DiagnosisDiseaseModel[]> {
    const response: AxiosResponse<APIResponse<DiagnosisDiseaseModel[]>> = await this.api.post(
      '/spice-service/patient/diagnosis-details',
      { patientReference, type }
    );
    return response.data.entity || response.data.data || [];
  }

  async saveUpdateDiagnosis(request: any): Promise<DiagnosisDiseaseModel[]> {
    const response: AxiosResponse<APIResponse<DiagnosisDiseaseModel[]>> = await this.api.post(
      '/spice-service/patient/confirm-diagnosis',
      request
    );
    return response.data.entity || response.data.data || [];
  }

  async getMetaData(): Promise<any> {
    const response: AxiosResponse<APIResponse<any>> = await this.api.post('/spice-service/static-data/user-data');
    return response.data.entity || response.data.data || {};
  }
}

export const apiService = new ApiService();
