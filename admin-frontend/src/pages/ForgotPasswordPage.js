import React, { useState } from 'react';
import { Form, Input, Button, Card, Alert, Typography } from 'antd';
import { MailOutlined } from '@ant-design/icons';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import styled from 'styled-components';
import tnutLogo from '../assets/logo.png';

const { Title, Text } = Typography;

const ForgotPasswordContainer = styled.div`
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
`;

const ForgotPasswordCard = styled(Card)`
  width: 100%;
  max-width: 400px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  
  .ant-card-body {
    padding: 40px;
  }
`;

const ForgotPasswordHeader = styled.div`
  text-align: center;
  margin-bottom: 30px;
`;

const ForgotPasswordPage = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const { forgotPassword } = useAuth();

  const onFinish = async (values) => {
    setLoading(true);
    setError('');
    setMessage('');
    
    const result = await forgotPassword(values.email);
    
    if (result.success) {
      setMessage('Kiểm tra email của bạn để đặt lại mật khẩu.');
    } else {
      setError(result.error);
    }
    
    setLoading(false);
  };

  return (
    <ForgotPasswordContainer>
      <ForgotPasswordCard>
        <ForgotPasswordHeader>
          <img src={tnutLogo} alt="TNUT Logo" style={{ width: '100px', marginBottom: '20px' }} />
          <Title level={2} style={{ color: '#1890ff', marginBottom: 8 }}>
            Quên Mật Khẩu
          </Title>
          <Text type="secondary">Bảng Điều Khiển Quản Trị</Text>
        </ForgotPasswordHeader>

        {error && (
          <Alert
            message={error}
            type="error"
            showIcon
            style={{ marginBottom: 20 }}
          />
        )}

        {message && (
          <Alert
            message={message}
            type="success"
            showIcon
            style={{ marginBottom: 20 }}
          />
        )}

        <Form
          name="forgot_password"
          onFinish={onFinish}
          autoComplete="off"
          size="large"
        >
          <Form.Item
            name="email"
            rules={[
              { required: true, message: 'Vui lòng nhập email!' },
              { type: 'email', message: 'Email không hợp lệ!' }
            ]}
          >
            <Input
              prefix={<MailOutlined />}
              placeholder="Email"
              autoComplete="email"
            />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
            >
              Gửi Yêu Cầu
            </Button>
          </Form.Item>
          <Form.Item>
            <Link to="/login">Quay lại trang đăng nhập</Link>
          </Form.Item>
        </Form>
      </ForgotPasswordCard>
    </ForgotPasswordContainer>
  );
};

export default ForgotPasswordPage;