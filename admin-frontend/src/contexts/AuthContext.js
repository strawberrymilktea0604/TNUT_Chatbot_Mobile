import React, { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Check if user is already logged in when app starts
  useEffect(() => {
    const token = localStorage.getItem('adminToken');
    const userData = localStorage.getItem('adminUser');
    
    if (token && userData) {
      setIsAuthenticated(true);
      setUser(JSON.parse(userData));
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    try {
      // Mock authentication - replace with real API call
      if (email === 'admin@tnut.edu.vn' && password === 'admin123') {
        const mockUser = {
          id: 1,
          email: 'admin@tnut.edu.vn',
          name: 'Quản Trị Viên',
          role: 'admin',
          avatar: null,
          createdAt: new Date().toISOString()
        };
        
        const mockToken = 'mock-jwt-token-' + Date.now();
        
        localStorage.setItem('adminToken', mockToken);
        localStorage.setItem('adminUser', JSON.stringify(mockUser));
        
        setIsAuthenticated(true);
        setUser(mockUser);
        
        return { success: true, user: mockUser };
      } else {
        throw new Error('Email hoặc mật khẩu không đúng');
      }
    } catch (error) {
      return { success: false, error: error.message };
    }
  };

  const logout = () => {
    localStorage.removeItem('adminToken');
    localStorage.removeItem('adminUser');
    setIsAuthenticated(false);
    setUser(null);
  };

  const updateUser = (userData) => {
    const updatedUser = { ...user, ...userData };
    setUser(updatedUser);
    localStorage.setItem('adminUser', JSON.stringify(updatedUser));
  };

  const changePassword = async (currentPassword, newPassword) => {
    try {
      // Mock password change - replace with real API call
      if (currentPassword === 'admin123') {
        // In real implementation, you would send this to your API
        return { success: true, message: 'Mật khẩu đã được thay đổi thành công' };
      } else {
        throw new Error('Mật khẩu hiện tại không đúng');
      }
    } catch (error) {
      return { success: false, error: error.message };
    }
  };

  const register = async (email, password) => {
    try {
      // Mock registration - replace with real API call
      console.log('Registering user:', email);
      // In a real app, you would send a request to your backend to create a new user.
      // For this mock, we'll just return success.
      return { success: true };
    } catch (error) {
      return { success: false, error: error.message };
    }
  };

  const forgotPassword = async (email) => {
    try {
      // Mock forgot password - replace with real API call
      console.log('Forgot password for user:', email);
      // In a real app, you would send a request to your backend to initiate a password reset.
      // For this mock, we'll just return success.
      return { success: true };
    } catch (error) {
      return { success: false, error: error.message };
    }
  };

  const resetPassword = async (token, newPassword) => {
    try {
      // Mock reset password - replace with real API call
      console.log('Resetting password with token:', token);
      // In a real app, you would send the token and new password to your backend.
      return { success: true };
    } catch (error) {
      return { success: false, error: error.message };
    }
  };

  const value = {
    isAuthenticated,
    user,
    loading,
    login,
    logout,
    updateUser,
    changePassword,
    register,
    forgotPassword,
    resetPassword
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
