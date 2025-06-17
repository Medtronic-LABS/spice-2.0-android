# SPICE Medical Review Web Application

A React web application that replicates the Login and Medical Review functionality from the SPICE Android application.

## Features

- **Login Page**: User authentication with email/phone and password
- **Medical Review Page**: Comprehensive medical review interface with:
  - Patient information display
  - Patient examination with complaints and physical examinations
  - Diagnosis management with vital signs tracking
  - Medical review summary

## Getting Started

### Prerequisites

- Node.js (version 16 or higher)
- npm or yarn

### Installation

1. Navigate to the web-app directory:
   ```bash
   cd web-app
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm start
   ```

4. Open [http://localhost:3000](http://localhost:3000) to view it in the browser.

### Configuration

The application uses environment variables for configuration:

- `REACT_APP_API_BASE_URL`: Backend API base URL (default: http://localhost:8080)

### API Integration

The application is designed to work with the SPICE backend API endpoints:

- `/auth-service/session` - User authentication
- `/spice-service/patient/patient-status` - Patient status information
- `/spice-service/patient/diagnosis-details` - Diagnosis details
- `/spice-service/patient/confirm-diagnosis` - Save/update diagnosis

### Project Structure

```
src/
├── components/          # Reusable React components
├── contexts/           # React context providers
├── pages/              # Main page components
├── services/           # API service layer
├── types/              # TypeScript type definitions
└── index.tsx           # Application entry point
```

### Building for Production

```bash
npm run build
```

This builds the app for production to the `build` folder.

## Technology Stack

- React 18 with TypeScript
- React Router for navigation
- Axios for API communication
- CSS3 for styling (responsive design)

## Medical Review Workflow

1. **Login**: Authenticate with username/email and password
2. **Patient Examination**: Select presenting complaints and physical examinations
3. **Diagnosis**: Manage patient diagnoses and vital signs (weight, blood pressure)
4. **Summary**: Review all collected information and submit medical review

## Development Notes

This web application is a conversion of the Android SPICE medical application, focusing specifically on the core Login and Medical Review functionality. The UI design follows medical application best practices with a clean, professional interface suitable for healthcare professionals.
