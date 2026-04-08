# System Architecture - Registration & Profile Flow

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    REGISTRATION PROCESS                          │
└─────────────────────────────────────────────────────────────────┘

┌──────────────┐
│   User fills │
│  Registration│
│     Form     │
└──────┬───────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────┐
│  Frontend (React) - Register.jsx                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Form Data:                                            │   │
│  │ • Username                                            │   │
│  │ • Email                                               │   │
│  │ • Password                                            │   │
│  │ • Phone Number ✨                                     │   │
│  │ • Address ✨                                          │   │
│  │ • Role                                                │   │
│  └──────────────────────────────────────────────────────┘   │
└──────┬───────────────────────────────────────────────────────┘
       │
       │ POST /api/auth/register
       │ JSON: { username, email, password, phone, address, role }
       ▼
┌──────────────────────────────────────────────────────────────┐
│  Backend (Spring Boot) - AuthController                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ @PostMapping("/register")                             │   │
│  │ public ResponseEntity<AuthResponse> register(         │   │
│  │     @RequestBody RegisterRequest request)             │   │
│  └──────────────────────────────────────────────────────┘   │
└──────┬───────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────┐
│  AuthService.register()                                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ 1. Check if username/email exists                     │   │
│  │ 2. Create User entity                                 │   │
│  │ 3. Hash password (BCrypt)                             │   │
│  │ 4. Set phone & address                                │   │
│  │ 5. Save to database                                   │   │
│  │ 6. Generate JWT token                                 │   │
│  └──────────────────────────────────────────────────────┘   │
└──────┬───────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────┐
│  Database - 'users' Table                                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ INSERT INTO users (                                   │   │
│  │   username, email, password,                          │   │
│  │   first_name, last_name,                              │   │
│  │   phone, address,                                     │   │
│  │   department, position,                               │   │
│  │   role, is_active                                     │   │
│  │ ) VALUES (...)                                        │   │
│  └──────────────────────────────────────────────────────┘   │
└──────┬───────────────────────────────────────────────────────┘
       │
       │ Response: AuthResponse
       │ { id, username, email, role, token,
       │   firstName, lastName, phone, address }
       ▼
┌──────────────────────────────────────────────────────────────┐
│  Frontend - Register.jsx                                     │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Store in localStorage:                                │   │
│  │ - token                                               │   │
│  │ - user: {                                             │   │
│  │     id, username, email, role,                        │   │
│  │     firstName, lastName,                              │   │
│  │     phone, address                                    │   │
│  │   }                                                   │   │
│  └──────────────────────────────────────────────────────┘   │
└──────┬───────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────┐
│  Redirect to Dashboard                                       │
└───────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────┐
│                    PROFILE DISPLAY PROCESS                       │
└─────────────────────────────────────────────────────────────────┘

┌──────────────┐
│   User clicks│
│   "Profile"  │
└──────┬───────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────┐
│  Frontend (React) - Profile.jsx                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ useEffect(() => {                                     │   │
│  │   fetch(`/api/auth/users/${user.id}`)                 │   │
│  │ })                                                    │   │
│  └──────────────────────────────────────────────────────┘   │
└──────┬───────────────────────────────────────────────────────┘
       │
       │ GET /api/auth/users/{id}
       ▼
┌──────────────────────────────────────────────────────────────┐
│  AuthService.getUserById()                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ SELECT * FROM users WHERE id = ?                      │   │
│  │ Returns UserDTO with:                                 │   │
│  │ - firstName, lastName                                 │   │
│  │ - phone, address                                      │   │
│  │ - email, role                                         │   │
│  └──────────────────────────────────────────────────────┘   │
└──────┬───────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────┐
│  Profile Page Display                                        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Personal Information Card:                            │   │
│  │                                                       │   │
│  │ Email Address          Phone Number                  │   │
│  │ john@example.com       +1234567890 ✏️                │   │
│  │                                                       │   │
│  │ Address                                               │   │
│  │ 123 Main Street, City, State ✏️                       │   │
│  └──────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────┐
│                    PROFILE EDIT PROCESS                          │
└─────────────────────────────────────────────────────────────────┘

┌──────────────┐
│   User clicks│
│   "Edit"     │
└──────┬───────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────┐
│  Form becomes editable                                       │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ <input name="phone" value={profileData.phone} />     │   │
│  │ <input name="address" value={profileData.address} /> │   │
│  └──────────────────────────────────────────────────────┘   │
└──────┬───────────────────────────────────────────────────────┘
       │
       │ User modifies and clicks "Save"
       ▼
┌──────────────────────────────────────────────────────────────┐
│  Frontend - handleSave()                                     │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ PUT /api/auth/users/${user.id}                        │   │
│  │ Body: {                                               │   │
│  │   firstName: "John",                                  │   │
│  │   lastName: "Doe",                                    │   │
│  │   phone: "+9876543210",                               │   │
│  │   address: "456 New Street"                           │   │
│  │ }                                                     │   │
│  └──────────────────────────────────────────────────────┘   │
└──────┬───────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────┐
│  AuthService.updateProfile()                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ User user = userRepository.findById(userId);          │   │
│  │ user.setPhone(profileData.getPhone());                │   │
│  │ user.setAddress(profileData.getAddress());            │   │
│  │ userRepository.save(user);                            │   │
│  └──────────────────────────────────────────────────────┘   │
└──────┬───────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────┐
│  Database Update                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ UPDATE users                                          │   │
│  │ SET phone = ?, address = ?                            │   │
│  │ WHERE id = ?                                          │   │
│  │                                                       │   │
│  │ ✅ Data saved PERMANENTLY                             │   │
│  └──────────────────────────────────────────────────────┘   │
└──────┬───────────────────────────────────────────────────────┘
       │
       │ Returns updated UserDTO
       ▼
┌──────────────────────────────────────────────────────────────┐
│  Frontend Success                                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Message: "Profile updated successfully!"              │   │
│  │ Exit edit mode                                        │   │
│  │ Display updated data                                  │   │
│  └──────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────┘
```

## Component Interaction Map

```
┌────────────────────────────────────────────────────────────┐
│                    FRONTEND LAYERS                          │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │  Register    │    │   Profile    │    │   Settings   │ │
│  │    .jsx      │    │    .jsx      │    │    .jsx      │ │
│  │              │    │              │    │              │ │
│  │ • username   │    │ • Display    │    │ • Display    │ │
│  │ • email      │    │ • Edit       │    │ • Edit       │ │
│  │ • password   │    │ • Save       │    │ • Save       │ │
│  │ • phone ✨   │    │ • Cancel     │    │ • Icons      │ │
│  │ • address ✨ │    │              │    │              │ │
│  │ • role       │    │              │    │              │ │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘ │
│         │                   │                   │          │
│         │                   │                   │          │
│         └───────────────────┼───────────────────┘          │
│                             │                               │
│                    ┌────────▼────────┐                      │
│                    │  AuthContext    │                      │
│                    │                 │                      │
│                    │ • user state    │                      │
│                    │ • setAuth()     │                      │
│                    │ • localStorage  │                      │
│                    └────────┬────────┘                      │
│                             │                               │
└─────────────────────────────┼───────────────────────────────┘
                              │
                    HTTP Requests
                              │
┌─────────────────────────────┼───────────────────────────────┐
│                    BACKEND LAYERS                            │
├─────────────────────────────▼───────────────────────────────┤
│  ┌──────────────────────────────────────────────────────┐  │
│  │           AuthController.java                        │  │
│  │                                                      │  │
│  │ POST /api/auth/register                              │  │
│  │ POST /api/auth/login                                 │  │
│  │ GET  /api/auth/users/{id}                            │  │
│  │ PUT  /api/auth/users/{id}                            │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                   │
│                         ▼                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           AuthService.java                           │  │
│  │                                                      │  │
│  │ • register() - saves phone & address                 │  │
│  │ • login() - returns phone & address                  │  │
│  │ • getUserById() - fetches complete profile           │  │
│  │ • updateProfile() - updates phone & address          │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                   │
│                         ▼                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           UserRepository.java                        │  │
│  │                                                      │  │
│  │ Extends JpaRepository<User, Long>                    │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                   │
│                         ▼                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           Database - users table                     │  │
│  │                                                      │  │
│  │ Columns:                                             │  │
│  │ • id, username, email, password                      │  │
│  │ • first_name, last_name                              │  │
│  │ • phone ✨, address ✨                               │  │
│  │ • department, position                               │  │
│  │ • role, employee_id, is_active                       │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────┘
```

## Security Flow

```
┌────────────────────────────────────────────────────────────┐
│              Authentication & Authorization                 │
└────────────────────────────────────────────────────────────┘

Registration:
1. User submits form
2. Password hashed with BCrypt
3. User saved to database
4. JWT token generated
5. Token sent to frontend
6. Frontend stores token in localStorage

Login:
1. User submits credentials
2. Find user by username
3. Verify password with BCrypt
4. Check if user is active
5. Generate JWT token
6. Send token + user data to frontend

API Requests:
1. Frontend includes token in header:
   Authorization: Bearer <token>
2. JwtAuthenticationFilter validates token
3. Extract username and role from token
4. Spring Security grants access based on role
5. Controller method executes
6. Return response

Profile Update:
1. User must be authenticated (has valid token)
2. Token validated on every request
3. User can only update their own profile
4. Data validated before saving
5. Changes committed to database
```

## Database Relationships

```
┌─────────────────┐
│   departments   │
│                 │
│ PK id           │
│    name         │
│    description  │
└────────┬────────┘
         │
         │ FK
         │
┌────────▼────────┐      ┌─────────────────┐
│   employees     │      │     users       │
│                 │      │                 │
│ PK id           │◄─────┤ PK id           │
│    first_name   │      │    username     │
│    last_name    │      │    email        │
│    email        │      │    password     │
│    phone        │      │    role         │
│    department_id│      │    employee_id  │◄─── Same employee
│    designation  │      │    first_name   │──── Independent
│    salary       │      │    last_name    │
│    address      │      │    phone        │
└─────────────────┘      │    address      │
                         │    department   │
                         │    position     │
                         └─────────────────┘

Note: 
- employees table has address field (for employee records)
- users table has address field (for login/profile)
- These are INDEPENDENT fields
- Changing user profile does NOT affect employee record
```

---

**This architecture ensures:**
✅ Clean separation of concerns
✅ Secure authentication
✅ Permanent data storage
✅ No impact on employee data
✅ Scalable design
