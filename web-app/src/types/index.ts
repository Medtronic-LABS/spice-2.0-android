export interface LoginResponse {
  firstName?: string;
  lastName?: string;
  username?: string;
  isActive: boolean;
  roles?: UserRole[];
  id: number;
  authorization?: string;
  deviceInfoId?: number;
  countryCode?: string;
  country: CountryModel;
  currentDate: number;
  timezone?: TimeZoneModel;
  tenantId: number;
  cultureId?: number;
  culture?: CulturesEntity;
  organizations?: OrganizationModel[];
  isSuperUser: boolean;
  isTermsAndConditionsAccepted?: boolean;
  suiteAccess: string[];
  client: string;
  phoneNumber?: string;
}

export interface UserRole {
  id: number;
  name: string;
  level: number;
  authority: string;
}

export interface CountryModel {
  id: number;
  name: string;
  countryCode?: string;
  phoneNumberCode?: string;
  unitMeasurement?: string;
  tenantId?: number;
}

export interface TimeZoneModel {
  id: number;
  offset?: string;
  description: string;
}

export interface CulturesEntity {
  id: number;
  name: string;
}

export interface OrganizationModel {
  id: number;
  name: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface APIResponse<T> {
  status: boolean;
  message?: string;
  entity?: T;
  data?: T;
}

export interface PatientStatusResponse {
  status?: string;
}

export interface DiagnosisDiseaseModel {
  id: number;
  diseaseCategory: string;
  name: string;
  value: string;
}

export interface ChipViewItemModel {
  name: string;
  cultureValue: string;
  type: string;
  selected?: boolean;
}

export interface PatientDetailModel {
  id?: string;
  patientId?: string;
  firstName?: string;
  lastName?: string;
  age?: number;
  gender?: string;
  phoneNumber?: string;
  nationalId?: string;
  programId?: string;
}

export interface MedicalReviewData {
  patientDetails?: PatientDetailModel;
  patientStatus?: PatientStatusResponse;
  diagnoses?: DiagnosisDiseaseModel[];
  complaints?: ChipViewItemModel[];
  examinations?: ChipViewItemModel[];
}

export interface AuthContextType {
  user: LoginResponse | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  loading: boolean;
  error: string | null;
}
