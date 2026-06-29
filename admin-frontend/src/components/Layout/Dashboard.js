import React, { useState } from 'react';
import { Layout, Menu, Avatar, Dropdown, Typography, Space, Badge } from 'antd';
import {
  DashboardOutlined,
  UserOutlined,
  FileTextOutlined,
  SettingOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  BellOutlined
} from '@ant-design/icons';
import { useAuth } from '../../contexts/AuthContext';
import { useNavigate, useLocation } from 'react-router-dom';
import DashboardHome from '../../pages/DashboardHome';
import tnutLogo from '../../assets/logo.png';

const { Header, Sider, Content } = Layout;
const { Title } = Typography;

const Dashboard = ({ children }) => {
  const [collapsed, setCollapsed] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const menuItems = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: 'Trang Chủ',
    },
    {
      key: '/accounts',
      icon: <UserOutlined />,
      label: 'Quản Lý Tài Khoản',
    },
    {
      key: '/documents',
      icon: <FileTextOutlined />,
      label: 'Tải Lên Tài Liệu',
    },
    {
      key: '/profile',
      icon: <SettingOutlined />,
      label: 'Thông Tin Cá Nhân',
    },
  ];

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: 'Thông tin cá nhân',
      onClick: () => navigate('/profile'),
    },
    {
      key: 'change-password',
      icon: <SettingOutlined />,
      label: 'Đổi mật khẩu',
      onClick: () => navigate('/change-password'),
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Đăng xuất',
      onClick: handleLogout,
    },
  ];

  const onMenuClick = ({ key }) => {
    navigate(key);
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider trigger={null} collapsible collapsed={collapsed} theme="dark">
        <div className="logo" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '16px 0' }}>
          <img src={tnutLogo} alt="TNUT Logo" style={{ height: '32px', marginRight: collapsed ? '0' : '8px' }} />
          {!collapsed && <span style={{ color: 'white', fontSize: '16px', fontWeight: 'bold' }}>TNUT Chat</span>}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={onMenuClick}
        />
      </Sider>
      
      <Layout>
        <Header style={{ 
          padding: '0 16px', 
          background: '#fff', 
          display: 'flex', 
          alignItems: 'center',
          justifyContent: 'space-between',
          boxShadow: '0 1px 4px rgba(0,21,41,.08)'
        }}>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            {React.createElement(
              collapsed ? MenuUnfoldOutlined : MenuFoldOutlined,
              {
                className: 'trigger',
                onClick: () => setCollapsed(!collapsed),
                style: { fontSize: '18px', cursor: 'pointer' }
              }
            )}
            <Title level={4} style={{ margin: '0 0 0 16px', color: '#1890ff' }}>
              Bảng Điều Khiển Quản Trị
            </Title>
          </div>

          <Space size="middle">
            <Badge count={3} size="small">
              <BellOutlined style={{ fontSize: '18px', cursor: 'pointer' }} />
            </Badge>
            
            <Dropdown
              menu={{ items: userMenuItems }}
              placement="bottomRight"
            >
              <div style={{ 
                display: 'flex', 
                alignItems: 'center', 
                cursor: 'pointer',
                padding: '4px 8px',
                borderRadius: '6px',
                transition: 'background-color 0.3s'
              }}>
                <Avatar 
                  size="small" 
                  icon={<UserOutlined />} 
                  style={{ marginRight: 8 }}
                />
                <span>{user?.name}</span>
              </div>
            </Dropdown>
          </Space>
        </Header>
        
        <Content style={{ 
          margin: '24px 16px', 
          padding: 24, 
          background: '#fff',
          borderRadius: '8px',
          minHeight: 280 
        }}>
          {children || <DashboardHome />}
        </Content>
      </Layout>
    </Layout>
  );
};

export default Dashboard;
