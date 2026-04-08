# Profile Page Edit & Save Guide ✅

## Overview
Your Profile page has a fully functional edit system that allows you to update your phone number and address permanently in the MySQL database.

---

## How to Use

### Step 1: Navigate to Profile Page
1. Login to your account
2. Click on **"Profile"** in the sidebar menu
3. You'll see your profile information displayed

---

### Step 2: View Mode (Default)

When you first open the profile page, you'll see:

```
┌─────────────────────────────────────┐
│  My Profile                         │
│  Manage your personal information   │
├─────────────────────────────────────┤
│                                     │
│  👤 John Doe                        │
│  Software Engineer                  │
│                                     │
│  Personal Information        [Edit] │
│                                     │
│  📄 Full Name      John Doe         │
│  ✉️ Email Address  john@example.com │
│  📞 Phone Number   +1 234 567 890   │
│  📍 Address        123 Main St      │
│                                     │
└─────────────────────────────────────┘
```

**Features:**
- All fields are **read-only** (display only)
- **Edit** button is visible in top-right
- Phone and address show your saved data from database

---

### Step 3: Click Edit Button

Click the **[Edit]** button in the top-right corner.

The page will change to **Edit Mode**:

```
┌─────────────────────────────────────┐
│  My Profile                         │
├─────────────────────────────────────┤
│                                     │
│  Personal Information    [Save] [Cancel] │
│                                     │
│  📄 Full Name      John Doe         │
│  ✉️ Email Address  john@example.com │
│  📞 Phone Number   [+1 234 567 890] ← EDITABLE │
│  📍 Address        [456 Oak Ave]    ← EDITABLE │
│                                     │
└─────────────────────────────────────┘
```

**Changes:**
- ✏️ **Phone Number** becomes an input field
- ✏️ **Address** becomes an input field
- 🔒 **Full Name** and **Email** remain read-only (can't be changed)
- **[Edit]** button is replaced with **[Save]** and **[Cancel]** buttons

---

### Step 4: Update Your Information

**Edit the fields:**

1. **Phone Number Field:**
   - Click in the phone field
   - Type your new phone number
   - Example: `+1-555-123-4567` or `(555) 123-4567`

2. **Address Field:**
   - Click in the address field
   - Type your new address
   - Example: `789 Maple Street, Apt 4B, Springfield, IL 62701`

**Example:**
```
Before:
📞 Phone: +1 234 567 890
📍 Address: 123 Main Street, City

After typing:
📞 Phone: +1-555-987-6543
📍 Address: 456 Oak Avenue, Apartment 12, New York, NY 10001
```

---

### Step 5: Save Changes Permanently

**Click the [Save] button** to save your changes.

**What happens:**
1. ✅ Data is sent to backend via API
2. ✅ Backend validates the data
3. ✅ Database is updated (MySQL `users` table)
4. ✅ Success message appears: "Profile updated successfully!"
5. ✅ Page returns to View Mode
6. ✅ Your changes are now visible permanently

**API Call Details:**
```javascript
PUT /api/auth/users/{yourUserId}
Headers:
  Authorization: Bearer {your-jwt-token}
  Content-Type: application/json
Body:
{
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1-555-987-6543",    // ← Your new phone
  "address": "456 Oak Avenue..."  // ← Your new address
}
```

---

### Step 6: Verify Changes Saved

**Method 1: Refresh Page**
1. Press F5 or click refresh button
2. Your new phone and address should still be displayed
3. This confirms data is saved permanently

**Method 2: Check Database Directly**
```sql
-- Run this in MySQL
SELECT id, username, phone, address 
FROM users 
WHERE id = YOUR_USER_ID;
```

You should see your updated values!

---

## Features Breakdown

### Edit Mode Fields

| Field | Editable? | Notes |
|-------|-----------|-------|
| Full Name | ❌ No | Read-only for security |
| Email | ❌ No | Read-only for security |
| **Phone Number** | ✅ **Yes** | Can be updated anytime |
| **Address** | ✅ **Yes** | Can be updated anytime |
| Department | ❌ No | Managed by admin |
| Position | ❌ No | Managed by admin |

---

## Buttons Explained

### [Edit] Button
- **Location:** Top-right of Personal Information section
- **Action:** Enables editing mode
- **Color:** Blue (#4f46e5)
- **Icon:** ✏️ Pencil icon

### [Save] Button
- **Location:** Replaces Edit button when in edit mode
- **Action:** Saves changes to database
- **Color:** Green (#10b981)
- **Icon:** 💾 Save icon
- **Loading State:** Shows "Saving..." while processing

### [Cancel] Button
- **Location:** Next to Save button
- **Action:** Discards changes and reloads page
- **Color:** Red (#ef4444)
- **Icon:** ❌ X icon
- **Warning:** Any unsaved changes will be lost

---

## Success & Error Messages

### ✅ Success Message
Appears after successful save:
```
┌─────────────────────────────────┐
│ ✓ Profile updated successfully! │
└─────────────────────────────────┘
```
- Background: Light green
- Text: Dark green
- Auto-hides after 3 seconds

### ❌ Error Messages

**Invalid Data:**
```
┌──────────────────────────────────┐
│ ✗ Failed to update profile       │
└──────────────────────────────────┘
```

**Network Error:**
```
┌──────────────────────────────────┐
│ ✗ An error occurred while        │
│   updating profile               │
└──────────────────────────────────┘
```

---

## Technical Details

### Frontend Files
- **Location:** `src/main/resources/hrmsystem-frontend/src/pages/Profile.jsx`
- **Key Functions:**
  - `handleInputChange()` - Updates form state as you type
  - `handleSave()` - Sends PUT request to backend
  - `handleCancel()` - Reloads page to reset data

### Backend Files
- **Controller:** `AuthController.java` - `@PutMapping("/users/{id}")`
- **Service:** `AuthService.java` - `updateProfile()` method
- **Entity:** `User.java` - Contains phone and address fields
- **Repository:** `UserRepository.java` - Extends JpaRepository

### Database Table
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(255),      -- ← Updated here
    address VARCHAR(255),    -- ← Updated here
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    department VARCHAR(255),
    position VARCHAR(255)
    -- ... other fields
);
```

---

## Testing Checklist

Test your profile editing with these scenarios:

### Test 1: Basic Update
- [ ] Go to Profile page
- [ ] Click Edit
- [ ] Change phone to: `+1-555-TEST-001`
- [ ] Change address to: `Test Address 123`
- [ ] Click Save
- [ ] See success message
- [ ] Refresh page
- [ ] Verify changes persisted ✅

### Test 2: Empty Values
- [ ] Click Edit
- [ ] Clear phone field completely
- [ ] Clear address field completely
- [ ] Click Save
- [ ] Should save empty values ✅
- [ ] Display shows empty or placeholder ✅

### Test 3: Special Characters
- [ ] Click Edit
- [ ] Enter phone with special chars: `+1 (555) 123-4567 ext. 89`
- [ ] Enter address with special chars: `123 O'Brien Street, Apt #4B`
- [ ] Click Save
- [ ] Should save correctly ✅
- [ ] Refresh and verify special chars display ✅

### Test 4: Cancel Function
- [ ] Click Edit
- [ ] Change phone and address
- [ ] Click Cancel (don't save)
- [ ] Page reloads
- [ ] Original values restored ✅
- [ ] Changes discarded ✅

---

## Common Issues & Solutions

### Issue 1: Changes Not Saving
**Symptoms:** Save button clicked but no success message

**Solutions:**
1. Check browser console for errors (F12 → Console tab)
2. Verify you're logged in (check if token exists)
3. Check Network tab for failed API calls
4. Try logging out and back in

### Issue 2: Fields Not Showing
**Symptoms:** Can't see phone/address fields

**Solutions:**
1. Hard refresh browser (Ctrl + Shift + R)
2. Clear browser cache
3. Check if you're on correct page (/dashboard/profile)
4. Verify user data loaded (check console logs)

### Issue 3: Edit Button Not Working
**Symptoms:** Click Edit but nothing happens

**Solutions:**
1. Check browser console for JavaScript errors
2. Clear browser cache
3. Try different browser
4. Verify React app loaded completely

---

## Security Notes

### What's Protected:
- ✅ **Username** - Cannot be changed (security)
- ✅ **Email** - Cannot be changed (used for login)
- ✅ **Role** - Cannot be changed (admin setting)
- ✅ **Department/Position** - HR managed fields

### What You Can Change:
- ✅ **Phone Number** - Your contact number
- ✅ **Address** - Your mailing address
- ✅ **First Name** - Your first name
- ✅ **Last Name** - Your last name

---

## Best Practices

### Phone Number Format:
While any format works, recommended formats:
- International: `+1-555-123-4567`
- US Standard: `(555) 123-4567`
- Simple: `555-123-4567`

### Address Format:
Include complete address:
```
Street Address
Apartment/Suite (if applicable)
City, State ZIP
Country (if international)
```

**Example:**
```
123 Main Street, Apt 4B
Springfield, IL 62701
United States
```

---

## Quick Reference

### How to Update Phone & Address:
1. Go to Profile page
2. Click **[Edit]** button
3. Modify **Phone Number** field
4. Modify **Address** field
5. Click **[Save]** button
6. ✅ Done! Changes saved permanently

### Keyboard Shortcuts:
- **Tab** - Move between fields
- **Shift+Tab** - Move back
- **Enter** - Triggers Save (when in edit mode)
- **Esc** - Cancel editing (in some browsers)

---

## Status

✅ **Fully Functional**
- Edit button works
- Phone field editable
- Address field editable
- Save persists to MySQL
- Cancel discards changes
- Success/error messages display
- Data loads from database on page load

---

**Last Updated:** March 26, 2026  
**Status:** ✅ Production Ready  
**Database:** MySQL - `users` table  
**Backend:** Spring Boot REST API  
**Frontend:** React with Vite
