# Email Verification Setup Guide

This guide explains how to set up email sending for the email verification feature in ChaceGo.

## Current Implementation

The app currently generates and stores verification codes in Firestore, but **email sending is not yet implemented**. The verification code is logged to the console for development/testing purposes.

## How It Works

1. User enters email address
2. System generates a 6-digit verification code
3. Code is stored in Firestore with 10-minute expiration
4. **YOU NEED TO IMPLEMENT**: Email sending with the code
5. User enters the code
6. System verifies the code
7. User creates password
8. Account is created

## Options for Email Sending

### Option 1: Firebase Cloud Functions (Recommended)

Create a Cloud Function that sends emails when verification codes are generated:

1. **Install Firebase CLI** (if not already installed):
   ```bash
   npm install -g firebase-tools
   ```

2. **Initialize Cloud Functions**:
   ```bash
   firebase init functions
   ```

3. **Install email library** (e.g., nodemailer):
   ```bash
   cd functions
   npm install nodemailer
   ```

4. **Create a Cloud Function** to send emails:
   ```javascript
   const functions = require('firebase-functions');
   const admin = require('firebase-admin');
   const nodemailer = require('nodemailer');
   
   admin.initializeApp();
   
   const transporter = nodemailer.createTransport({
     service: 'gmail', // or your email service
     auth: {
       user: functions.config().email.user,
       pass: functions.config().email.password
     }
   });
   
   exports.sendVerificationEmail = functions.firestore
     .document('email_verifications/{email}')
     .onCreate(async (snap, context) => {
       const { email, code } = snap.data();
       
       const mailOptions = {
         from: 'noreply@chacego.com',
         to: email,
         subject: 'Votre code de vérification ChaceGo',
         html: `
           <h2>Code de vérification</h2>
           <p>Votre code de vérification est: <strong>${code}</strong></p>
           <p>Ce code expire dans 10 minutes.</p>
         `
       };
       
       return transporter.sendMail(mailOptions);
     });
   ```

5. **Update EmailVerificationService.kt**:
   - The Cloud Function will automatically trigger when a document is created
   - No changes needed to the Android code - Firestore triggers handle it

### Option 2: Backend API

Create a backend endpoint that sends emails:

1. **Create an API endpoint** (e.g., using Node.js, Python, etc.):
   ```javascript
   POST /api/send-verification-code
   Body: { email: "user@example.com", code: "123456" }
   ```

2. **Update EmailVerificationService.kt**:
   ```kotlin
   // In sendVerificationCode function, after storing in Firestore:
   sendEmailViaApi(email, code)
   ```

3. **Add HTTP client dependency** (e.g., Retrofit):
   ```kotlin
   // In build.gradle.kts
   implementation("com.squareup.retrofit2:retrofit:2.9.0")
   ```

### Option 3: Third-Party Email Service

Use services like:
- **SendGrid**
- **Mailgun**
- **Amazon SES**
- **EmailJS** (for frontend)

Example with EmailJS (simpler, no backend needed):
1. Sign up at emailjs.com
2. Get API credentials
3. Add HTTP client to Android app
4. Call EmailJS API from `EmailVerificationService.kt`

## Firestore Security Rules

Make sure your Firestore security rules allow reading/writing to the `email_verifications` collection:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /email_verifications/{email} {
      // Allow write (to create verification codes)
      allow create: if request.auth == null;
      // Allow read only if user knows the email (in practice, verify via server)
      allow read: if request.resource.data.email == request.auth.token.email;
    }
  }
}
```

## Testing Without Email Service

For development/testing, the verification code is logged to Android Logcat:
```
D/EmailVerification: Verification code for user@example.com: 123456
```

You can check Logcat to see the code and enter it manually.

## Important Notes

⚠️ **Security Considerations**:
- Codes expire after 10 minutes
- Codes are marked as used after verification
- Consider rate limiting to prevent abuse
- Consider adding CAPTCHA to prevent bot abuse

📧 **Email Delivery**:
- Ensure your email service has high deliverability
- Consider using a transactional email service
- Add proper email templates
- Handle bounce/error cases

🔒 **Production**:
- Never log verification codes in production
- Use environment variables for email credentials
- Monitor email sending failures
- Implement proper error handling

