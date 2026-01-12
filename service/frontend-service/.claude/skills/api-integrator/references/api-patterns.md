# TypeScript API Integration Patterns

## Core Principles

1. **Always use API Gateway**: All requests go through `http://localhost:8080`
2. **Token management**: Support both `userToken` (market) and `adminToken` (admin)
3. **Type safety**: Generate TypeScript interfaces from Swagger schemas
4. **Error handling**: Consistent error handling with user-friendly messages
5. **Request/Response typing**: Fully typed request parameters and response data

## File Organization

API functions should be organized by service in the `src/api/` directory:

```
src/api/
├── authApi.ts      # auth-service APIs
├── userApi.ts      # user-service APIs
├── productApi.ts   # product-service APIs
├── catalogApi.ts   # catalog-service APIs
├── orderApi.ts     # order-service APIs
└── ...
```

If user specifies a different location, use that instead.

## Standard API Function Pattern

### Basic Structure

```typescript
// TypeScript interfaces (from Swagger schemas)
export interface RequestType {
  // fields from Swagger request body/params schema
}

export interface ResponseType {
  // fields from Swagger response schema
}

// API function
export const apiFunction = async (
  params: RequestType,
  tokenType?: 'user' | 'admin' | 'none'
): Promise<ResponseType> => {
  // Token retrieval
  const token = tokenType === 'admin'
    ? localStorage.getItem('adminToken')
    : tokenType === 'user'
    ? localStorage.getItem('userToken')
    : null

  // Request configuration
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  try {
    // API Gateway URL
    const response = await fetch(`http://localhost:8080/path/to/endpoint`, {
      method: 'POST', // or GET, PUT, DELETE, etc.
      headers,
      body: JSON.stringify(params), // omit for GET requests
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Request failed' }))
      throw new Error(error.message || `HTTP ${response.status}`)
    }

    const data: ResponseType = await response.json()
    return data
  } catch (error) {
    console.error('API Error:', error)
    throw error
  }
}
```

## Pattern Examples

### Example 1: GET Request with Path Parameters (Public API)

```typescript
// Swagger: GET /catalog/products/{productId}
// Response: { product_id: string, name: string, price: number, ... }

export interface Product {
  product_id: string
  name: string
  price: number
  description?: string
  image_url?: string
  stock: number
}

export const getProductById = async (productId: string): Promise<Product> => {
  try {
    const response = await fetch(`http://localhost:8080/catalog/products/${productId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    })

    if (!response.ok) {
      throw new Error(`Failed to fetch product: ${response.status}`)
    }

    const data: Product = await response.json()
    return data
  } catch (error) {
    console.error('Get product error:', error)
    throw error
  }
}
```

### Example 2: POST Request with Body (Authenticated - User Token)

```typescript
// Swagger: POST /orders
// Request: { product_id: string, quantity: number, address: string }
// Response: { order_id: string, status: string, total_amount: number }

export interface CreateOrderRequest {
  product_id: string
  quantity: number
  address: string
}

export interface CreateOrderResponse {
  order_id: string
  status: string
  total_amount: number
  created_at: string
}

export const createOrder = async (
  orderData: CreateOrderRequest
): Promise<CreateOrderResponse> => {
  const token = localStorage.getItem('userToken')

  if (!token) {
    throw new Error('Authentication required. Please login.')
  }

  try {
    const response = await fetch(`http://localhost:8080/orders`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify(orderData),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Order creation failed' }))
      throw new Error(error.message || `HTTP ${response.status}`)
    }

    const data: CreateOrderResponse = await response.json()
    return data
  } catch (error) {
    console.error('Create order error:', error)
    throw error
  }
}
```

### Example 3: GET Request with Query Parameters (Authenticated - Admin Token)

```typescript
// Swagger: GET /admin/users?page=1&size=10&role=CUSTOMER
// Response: { users: [...], total: number, page: number }

export interface User {
  user_id: string
  email: string
  name: string
  role: string
  created_at: string
}

export interface GetUsersResponse {
  users: User[]
  total: number
  page: number
  size: number
}

export const getUsers = async (
  page: number = 1,
  size: number = 10,
  role?: string
): Promise<GetUsersResponse> => {
  const token = localStorage.getItem('adminToken')

  if (!token) {
    throw new Error('Admin authentication required')
  }

  const queryParams = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
  })

  if (role) {
    queryParams.append('role', role)
  }

  try {
    const response = await fetch(
      `http://localhost:8080/admin/users?${queryParams.toString()}`,
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
      }
    )

    if (!response.ok) {
      throw new Error(`Failed to fetch users: ${response.status}`)
    }

    const data: GetUsersResponse = await response.json()
    return data
  } catch (error) {
    console.error('Get users error:', error)
    throw error
  }
}
```

### Example 4: PUT Request (Update)

```typescript
// Swagger: PUT /products/{productId}
// Request: { name?: string, price?: number, stock?: number }
// Response: { product_id: string, updated_at: string }

export interface UpdateProductRequest {
  name?: string
  price?: number
  stock?: number
  description?: string
}

export interface UpdateProductResponse {
  product_id: string
  updated_at: string
}

export const updateProduct = async (
  productId: string,
  updates: UpdateProductRequest
): Promise<UpdateProductResponse> => {
  const token = localStorage.getItem('adminToken')

  if (!token) {
    throw new Error('Admin authentication required')
  }

  try {
    const response = await fetch(`http://localhost:8080/products/${productId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify(updates),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Update failed' }))
      throw new Error(error.message || `HTTP ${response.status}`)
    }

    const data: UpdateProductResponse = await response.json()
    return data
  } catch (error) {
    console.error('Update product error:', error)
    throw error
  }
}
```

### Example 5: DELETE Request

```typescript
// Swagger: DELETE /coupons/{couponId}
// Response: { success: boolean, message: string }

export interface DeleteCouponResponse {
  success: boolean
  message: string
}

export const deleteCoupon = async (couponId: string): Promise<DeleteCouponResponse> => {
  const token = localStorage.getItem('adminToken')

  if (!token) {
    throw new Error('Admin authentication required')
  }

  try {
    const response = await fetch(`http://localhost:8080/coupons/${couponId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
    })

    if (!response.ok) {
      throw new Error(`Failed to delete coupon: ${response.status}`)
    }

    const data: DeleteCouponResponse = await response.json()
    return data
  } catch (error) {
    console.error('Delete coupon error:', error)
    throw error
  }
}
```

## Token Type Decision Guide

Use this guide to determine which token type to use:

- **Public APIs** (no token): Catalog browsing, product list, public notices
- **User Token** (localStorage 'userToken'): Orders, cart, user profile, mypage features
- **Admin Token** (localStorage 'adminToken'): Admin CRUD operations, user management, statistics

When generating API functions:
1. Check the Swagger endpoint path - if it starts with `/admin`, use admin token
2. If the endpoint requires authentication but is not admin, use user token
3. If the Swagger spec indicates no security requirement, use no token

## Error Handling Best Practices

1. **Network errors**: Wrap fetch in try-catch
2. **HTTP errors**: Check response.ok and parse error response
3. **Authentication errors**: Provide clear "please login" messages
4. **Validation errors**: Include field-specific error information from API response
5. **Logging**: Always log errors to console for debugging

## Response Parsing

Always specify the response type when calling `response.json()`:

```typescript
const data: ResponseType = await response.json()
```

This ensures TypeScript type checking and prevents type errors.
