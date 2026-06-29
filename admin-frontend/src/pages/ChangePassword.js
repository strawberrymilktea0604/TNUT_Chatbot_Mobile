import React, { useState } from 'react';
import { 
  Card, 
  Form, 
  Input, 
  Button, 
  message, 
  Typography,
  Space,
  Alert
} from 'antd';
import { 
  LockOutlined, 
  EyeInvisibleOutlined, 
  EyeTwoTone,
  SafetyOutlined
} from '@ant-design/icons';
import { useAuth } from '../contexts/AuthContext';

const { Title, Text } = Typography;

const ChangePassword = () => {
  const [loading, setLoading] = useState(false);
  const { changePassword } = useAuth();
  const [form] = Form.useForm();

  const onFinish = async (values) => {
    setLoading(true);
    
    const result = await changePassword(values.currentPassword, values.newPassword);
    
    if (result.success) {
      message.success(result.message);
      form.resetFields();
    } else {
      message.error(result.error);
    }
    
    setLoading(false);
  };

  const validateConfirmPassword = (_, value) => {
    if (!value || form.getFieldValue('newPassword') === value) {
      return Promise.resolve();
    }
    return Promise.reject(new Error('Mật khẩu xác nhận không khớp!'));
  };

  const validatePasswordStrength = (_, value) => {
    if (!value) {
      return Promise.resolve();
    }
    
    const minLength = value.length >= 8;
    const hasUpperCase = /[A-Z]/.test(value);
    const hasLowerCase = /[a-z]/.test(value);
    const hasNumbers = /\d/.test(value);
    const hasSpecialChar = /[!@#$%^&*(),.?":{}|<>]/.test(value);
    
    if (minLength && hasUpperCase && hasLowerCase && hasNumbers) {
      return Promise.resolve();
    }
    
    return Promise.reject(new Error('Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số!'));
  };

  const getPasswordStrength = (password) => {
    if (!password) return { strength: 0, text: '', color: '' };
    
    let score = 0;
    const checks = [
      password.length >= 8,
      /[A-Z]/.test(password),
      /[a-z]/.test(password),
      /\d/.test(password),
      /[!@#$%^&*(),.?":{}|<>]/.test(password)
    ];
    
    score = checks.filter(Boolean).length;
    
    if (score <= 2) {
      return { strength: score * 20, text: 'Yếu', color: '#ff4d4f' };
    } else if (score <= 3) {
      return { strength: score * 20, text: 'Trung bình', color: '#faad14' };
    } else if (score <= 4) {
      return { strength: score * 20, text: 'Tốt', color: '#52c41a' };
    } else {
      return { strength: 100, text: 'Rất tốt', color: '#52c41a' };
    }
  };

  const [passwordStrength, setPasswordStrength] = useState({ strength: 0, text: '', color: '' });

  const handlePasswordChange = (e) => {
    const password = e.target.value;
    setPasswordStrength(getPasswordStrength(password));
  };

  return (
    <div style={{ maxWidth: 600, margin: '0 auto' }}>
      <Title level={2}>Đổi Mật Khẩu</Title>
      
      <Card>
        <Alert
          message="Lưu ý bảo mật"
          description="Để đảm bảo an toàn tài khoản, vui lòng chọn mật khẩu mạnh và không chia sẻ với người khác."
          type="info"
          showIcon
          icon={<SafetyOutlined />}
          style={{ marginBottom: 24 }}
        />

        <Form
          form={form}
          name="change_password"
          onFinish={onFinish}
          layout="vertical"
          autoComplete="off"
        >
          <Form.Item
            name="currentPassword"
            label="Mật Khẩu Hiện Tại"
            rules={[{ required: true, message: 'Vui lòng nhập mật khẩu hiện tại!' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="Nhập mật khẩu hiện tại"
              iconRender={(visible) => (visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />)}
            />
          </Form.Item>

          <Form.Item
            name="newPassword"
            label="Mật Khẩu Mới"
            rules={[
              { required: true, message: 'Vui lòng nhập mật khẩu mới!' },
              { validator: validatePasswordStrength }
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="Nhập mật khẩu mới"
              onChange={handlePasswordChange}
              iconRender={(visible) => (visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />)}
            />
          </Form.Item>

          {passwordStrength.strength > 0 && (
            <div style={{ marginBottom: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                <Text>Độ mạnh mật khẩu:</Text>
                <Text style={{ color: passwordStrength.color }}>{passwordStrength.text}</Text>
              </div>
              <div style={{ 
                width: '100%', 
                height: 6, 
                backgroundColor: '#f0f0f0', 
                borderRadius: 3,
                overflow: 'hidden'
              }}>
                <div 
                  style={{ 
                    width: `${passwordStrength.strength}%`, 
                    height: '100%', 
                    backgroundColor: passwordStrength.color,
                    transition: 'all 0.3s'
                  }} 
                />
              </div>
            </div>
          )}

          <Form.Item
            name="confirmPassword"
            label="Xác Nhận Mật Khẩu Mới"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: 'Vui lòng xác nhận mật khẩu mới!' },
              { validator: validateConfirmPassword }
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="Nhập lại mật khẩu mới"
              iconRender={(visible) => (visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />)}
            />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button 
                type="primary" 
                htmlType="submit" 
                loading={loading}
                size="large"
              >
                Đổi Mật Khẩu
              </Button>
              <Button 
                onClick={() => form.resetFields()}
                size="large"
              >
                Đặt Lại
              </Button>
            </Space>
          </Form.Item>
        </Form>

        <Card size="small" title="Yêu Cầu Mật Khẩu" style={{ marginTop: 24, backgroundColor: '#fafafa' }}>
          <ul style={{ margin: 0, paddingLeft: 20 }}>
            <li>Ít nhất 8 ký tự</li>
            <li>Bao gồm chữ hoa (A-Z)</li>
            <li>Bao gồm chữ thường (a-z)</li>
            <li>Bao gồm số (0-9)</li>
            <li>Nên có ký tự đặc biệt (!@#$%^&*)</li>
          </ul>
        </Card>

        <div style={{ marginTop: 16, padding: 12, backgroundColor: '#fff7e6', borderRadius: 6, border: '1px solid #ffd591' }}>
          <Text type="secondary" style={{ fontSize: '12px' }}>
            <strong>Mật khẩu demo hiện tại:</strong> admin123
          </Text>
        </div>
      </Card>
    </div>
  );
};

export default ChangePassword;
