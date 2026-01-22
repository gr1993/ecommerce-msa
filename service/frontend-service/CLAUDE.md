# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the frontend service for an e-commerce MSA (Microservices Architecture) system built with Spring Cloud. The frontend is developed using React with Vite and TypeScript, providing both customer-facing shopping mall interface and admin management pages. The codebase was developed using Cursor IDE with AI-assisted coding (vibe coding).

## Technology Stack

- **Build Tool**: Vite 7.2.4
- **Framework**: React 19.2.0
- **Language**: TypeScript
- **Routing**: react-router-dom 7.10.1
- **State Management**: Zustand 5.0.2 with persist middleware
- **UI Library**: Ant Design (antd 6.1.0) for tables and components
- **Charts**: @ant-design/charts for data visualization
- **Rich Text Editor**: react-quill-new 3.6.0 (used instead of react-quill due to React 19 removing ReactDOM.findDOMNode)

## Common Commands

### Development
```bash
npm run dev          # Start development server
```

### Build
```bash
npm run build        # TypeScript compilation and Vite build
```

### Linting
```bash
npm run lint         # Run ESLint
```

### Preview
```bash
npm preview          # Preview production build locally
```

## TypeScript Import Guidelines

### Type-Only Imports

In this project, all TypeScript types or interfaces imported from other modules **must use `type`-only imports**. This prevents errors when `verbatimModuleSyntax` is enabled.

#### Correct Usage

```ts
// Importing a type/interface only
import type { CategoryTreeNode, Product } from "@/types/catalog";

// Example usage
const node: CategoryTreeNode = {
  categoryId: 1,
  parentId: null,
  categoryName: "상의",
  displayOrder: 0,
  isDisplayed: true,
  depth: 1,
  children: []
};
```

## Architecture

### Dual-Interface Architecture

This frontend serves **two distinct user interfaces** in a single application:

1. **Market (Customer-Facing)**: `/market/*` routes
   - Shopping interface for end users
   - Product browsing, cart, checkout, mypage
   - Public and authenticated user sections

2. **Admin (Management)**: `/admin/*` routes
   - Management console for administrators
   - Separate authentication flow
   - Full CRUD operations for products, orders, users, etc.

### Routing Structure

Routes are centralized in `src/App.tsx`:

- **Market Routes** (`/market/*`):
  - Main, Product List/Detail, Cart, Order, Order Complete
  - MyPage nested routes (orders, shipping, returns, coupons, points, profile)
  - Login/Signup, Support/Notices

- **Admin Routes** (`/admin/*`):
  - Uses `AdminLayout` wrapper with sidebar navigation
  - Dashboard, User Management, Product Management
  - Catalog (category, display, search keywords)
  - Order & Payment, Shipping (delivery, return, exchange)
  - Operations (coupons, discounts, notices)
  - Settlement (manage, revenue statistics)

### Component Organization

```
src/
├── components/
│   └── market/           # Reusable market components (Header, Footer)
├── pages/
│   ├── admin/            # Admin pages with nested subfolders
│   │   ├── AdminLayout.tsx      # Admin sidebar layout wrapper
│   │   ├── AdminLogin.tsx       # Separate admin authentication
│   │   ├── product/
│   │   ├── catalog/
│   │   ├── order/
│   │   ├── shipping/
│   │   ├── operation/
│   │   ├── settlement/
│   │   └── user/
│   └── market/           # Market pages with nested subfolders
│       ├── mypage/       # User account pages
│       └── support/      # Customer support pages
├── stores/               # Zustand global state stores
│   ├── authStore.ts      # Authentication state (user & admin)
│   └── cartStore.ts      # Shopping cart state
└── utils/                # Shared utility functions
```

### State Management

**Zustand-based**: Uses Zustand for reactive global state management with localStorage persistence.

- **Authentication Store** (`src/stores/authStore.ts`):
  - Manages both market user and admin authentication
  - Market auth: `userToken`, `user` state
  - Admin auth: `adminToken`, `adminUser` state
  - Separate authentication flows: `login()`, `logout()`, `adminLogin()`, `adminLogout()`
  - Auto-synced to localStorage via persist middleware
  - Usage: `const { user, login, isLoggedIn } = useAuthStore()`

- **Shopping Cart Store** (`src/stores/cartStore.ts`):
  - Manages shopping cart items with reactive updates
  - Actions: `addToCart()`, `removeFromCart()`, `updateQuantity()`, `clearCart()`
  - Computed values: `getCartItemCount()`, `getTotalPrice()`
  - Auto-synced to localStorage via persist middleware
  - Usage: `const { items, addToCart, removeFromCart } = useCartStore()`

- **Payment**: Utilities in `src/utils/paymentUtils.ts`

**Why Zustand?**
- Lightweight (~1KB) and zero boilerplate
- Automatic component re-rendering on state changes
- Built-in localStorage persistence
- Type-safe with TypeScript
- No Context Provider wrapper needed

### Styling Approach

Each page/component has its own **colocated CSS file**:
- Pattern: `ComponentName.tsx` → `ComponentName.css`
- Example: `AdminLayout.tsx` + `AdminLayout.css`

No global CSS framework or CSS-in-JS solution. Ant Design provides base component styling.

## MSA Backend Context

This frontend is part of a larger Spring Cloud MSA system with:

- **Gateway Service**: Authentication, rate limiting, circuit breaker
- **Discovery Service**: Service registry (Eureka)
- **Auth Service**: JWT token issuance and validation
- **Domain Services**: User, Product, Catalog, Order, Payment, Promotion, Delivery, Settlement, Return services

### Key Microservices Integration Notes

- **Product vs Catalog**: Product-Service manages inventory (admin), Catalog-Service handles customer-facing display (optimized reads with caching/search)
- **Order Flow**: Order creation → Payment verification → Inventory reservation → Stock deduction (uses Saga Pattern for distributed transactions)
- **Event-Driven**: Services communicate via Kafka events (e.g., product updates trigger catalog sync)

## Development Guidelines

### When Working with Admin Pages

- Admin pages use `AdminLayout` which provides sidebar navigation
- Admin authentication is separate from market authentication
- Check localStorage for `adminToken` and `adminUser`
- Most admin pages use Ant Design Table component

### When Working with Market Pages

- Market pages have standalone layouts (MarketHeader + page content)
- Use `useAuthStore()` hook for authentication: `const { isLoggedIn, user } = useAuthStore()`
- Use `useCartStore()` hook for cart operations: `const { items, addToCart } = useCartStore()`
- Market uses customer-facing language (Korean)

### React 19 Compatibility

- Use `react-quill-new` instead of `react-quill` (React 19 removed ReactDOM.findDOMNode)
- Ensure any new libraries are compatible with React 19

### Vite Configuration

- Standard Vite + React setup in `vite.config.ts`
- No special proxy or path alias configuration currently

## TypeScript Configuration

- `tsconfig.json` - Base config
- `tsconfig.app.json` - App-specific (includes src/)
- `tsconfig.node.json` - Vite config files

## Adding New Features

1. **New Admin Page**: Add route in `App.tsx` under `/admin`, create page in `src/pages/admin/[category]/`, add navigation link in `AdminLayout.tsx` sidebar
2. **New Market Page**: Add route in `App.tsx` under `/market`, create page in `src/pages/market/`, add navigation in `MarketHeader.tsx` if needed
3. **New Utility**: Add to appropriate file in `src/utils/` or create new utility module

## Important Notes

- The parent directory (`../../`) contains other microservices (auth-service, gateway-service, etc.)
- Root `README.md` provides architecture overview of entire MSA system
- This frontend communicates with backend services through the Gateway Service
