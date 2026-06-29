import React, { useState } from 'react';
import { 
  Card, 
  Form, 
  Input, 
  Button, 
  Avatar, 
  Typography, 
  Row, 
  Col,
  message,
  Divider,
  Space,
  Upload
} from 'antd';
import { 
  UserOutlined, 
  MailOutlined, 
  PhoneOutlined,
  CalendarOutlined,
  EditOutlined,
  CameraOutlined
} from '@ant-design/icons';
import { useAuth } from '../contexts/AuthContext';

const { Title, Text } = Typography;

const Profile = () => {
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(false);
  const { user, updateUser } = useAuth();
  const [form] = Form.useForm();

  const handleEdit = () => {
    setEditing(true);
    form.setFieldsValue({
      name: user.name,
      email: user.email,
      phone: user.phone || '',
      department: user.department || 'Phòng Công Nghệ Thông Tin',
      position: user.position || 'Quản Trị Viên Hệ Thống',
      bio: user.bio || 'Quản trị viên hệ thống chatbot TNUT'
    });
  };

  const handleSave = async () => {
    try {
      setLoading(true);
      const values = await form.validateFields();
      
      // Simulate API call
      setTimeout(() => {
        updateUser(values);
        setEditing(false);
        setLoading(false);
        message.success('Cập nhật thông tin thành công!');
      }, 1000);
    } catch (error) {
      setLoading(false);
      console.error('Validation failed:', error);
    }
  };

  const handleCancel = () => {
    setEditing(false);
    form.resetFields();
  };

  const handleAvatarUpload = (info) => {
    if (info.file.status === 'done') {
      message.success('Cập nhật avatar thành công!');
    } else if (info.file.status === 'error') {
      message.error('Cập nhật avatar thất bại!');
    }
  };

  const uploadProps = {
    name: 'avatar',
    action: '/api/upload/avatar', // Mock endpoint
    showUploadList: false,
    beforeUpload: (file) => {
      const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png';
      if (!isJpgOrPng) {
        message.error('Chỉ hỗ trợ file JPG/PNG!');
        return false;
      }
      const isLt2M = file.size / 1024 / 1024 < 2;
      if (!isLt2M) {
        message.error('Hình ảnh phải nhỏ hơn 2MB!');
        return false;
      }
      return false; // Prevent auto upload for demo
    },
    onChange: handleAvatarUpload,
  };

  return (
    <div>
      <Title level={2}>Thông Tin Cá Nhân</Title>

      <Row gutter={24}>
        <Col xs={24} lg={8}>
          <Card>
            <div style={{ textAlign: 'center' }}>
              <div style={{ position: 'relative', display: 'inline-block' }}>
                <Avatar 
                  size={120} 
                  icon={<UserOutlined />}
                  src={user.avatar}
                  style={{ marginBottom: 16 }}
                />
                <Upload {...uploadProps}>
                  <Button
                    shape="circle"
                    icon={<CameraOutlined />}
                    size="small"
                    style={{
                      position: 'absolute',
                      bottom: 16,
                      right: 0,
                      backgroundColor: '#1890ff',
                      color: 'white',
                      border: 'none'
                    }}
                  />
                </Upload>
              </div>
              
              <Title level={4} style={{ marginBottom: 4 }}>
                {user?.name}
              </Title>
              <Text type="secondary">{user?.role === 'admin' ? 'Quản Trị Viên' : user?.role}</Text>
              
              <Divider />
              
              <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                <div style={{ textAlign: 'left' }}>
                  <Text strong>
                    <MailOutlined style={{ marginRight: 8 }} />
                    Email:
                  </Text>
                  <br />
                  <Text>{user?.email}</Text>
                </div>
                
                <div style={{ textAlign: 'left' }}>
                  <Text strong>
                    <CalendarOutlined style={{ marginRight: 8 }} />
                    Ngày tham gia:
                  </Text>
                  <br />
                  <Text>{new Date(user?.createdAt).toLocaleDateString('vi-VN')}</Text>
                </div>
                
                <div style={{ textAlign: 'left' }}>
                  <Text strong>
                    <PhoneOutlined style={{ marginRight: 8 }} />
                    Số điện thoại:
                  </Text>
                  <br />
                  <Text>{user?.phone || 'Chưa cập nhật'}</Text>
                </div>
              </Space>
            </div>
          </Card>
        </Col>

        <Col xs={24} lg={16}>
          <Card 
            title="Thông Tin Chi Tiết"
            extra={
              !editing && (
                <Button 
                  type="primary" 
                  icon={<EditOutlined />}
                  onClick={handleEdit}
                >
                  Chỉnh Sửa
                </Button>
              )
            }
          >
            {editing ? (
              <Form
                form={form}
                layout="vertical"
                onFinish={handleSave}
              >
                <Row gutter={16}>
                  <Col xs={24} sm={12}>
                    <Form.Item
                      name="name"
                      label="Họ và Tên"
                      rules={[
                        { required: true, message: 'Vui lòng nhập họ tên!' },
                        { min: 2, message: 'Họ tên phải có ít nhất 2 ký tự!' }
                      ]}
                    >
                      <Input placeholder="Nhập họ và tên" />
                    </Form.Item>
                  </Col>
                  
                  <Col xs={24} sm={12}>
                    <Form.Item
                      name="email"
                      label="Email"
                      rules={[
                        { required: true, message: 'Vui lòng nhập email!' },
                        { type: 'email', message: 'Email không hợp lệ!' }
                      ]}
                    >
                      <Input placeholder="Nhập email" />
                    </Form.Item>
                  </Col>
                </Row>

                <Row gutter={16}>
                  <Col xs={24} sm={12}>
                    <Form.Item
                      name="phone"
                      label="Số Điện Thoại"
                      rules={[
                        { pattern: /^[0-9]{10,11}$/, message: 'Số điện thoại không hợp lệ!' }
                      ]}
                    >
                      <Input placeholder="Nhập số điện thoại" />
                    </Form.Item>
                  </Col>
                  
                  <Col xs={24} sm={12}>
                    <Form.Item
                      name="department"
                      label="Phòng Ban"
                    >
                      <Input placeholder="Nhập phòng ban" />
                    </Form.Item>
                  </Col>
                </Row>

                <Form.Item
                  name="position"
                  label="Chức Vụ"
                >
                  <Input placeholder="Nhập chức vụ" />
                </Form.Item>

                <Form.Item
                  name="bio"
                  label="Giới Thiệu"
                >
                  <Input.TextArea 
                    rows={4} 
                    placeholder="Viết vài dòng giới thiệu về bản thân"
                  />
                </Form.Item>

                <Form.Item>
                  <Space>
                    <Button 
                      type="primary" 
                      htmlType="submit"
                      loading={loading}
                    >
                      Lưu Thay Đổi
                    </Button>
                    <Button onClick={handleCancel}>
                      Hủy
                    </Button>
                  </Space>
                </Form.Item>
              </Form>
            ) : (
              <div>
                <Row gutter={[16, 16]}>
                  <Col span={24}>
                    <Card size="small" title="Thông Tin Cơ Bản">
                      <Row gutter={16}>
                        <Col xs={24} sm={12}>
                          <div style={{ marginBottom: 16 }}>
                            <Text strong>Họ và Tên:</Text>
                            <br />
                            <Text>{user?.name}</Text>
                          </div>
                        </Col>
                        <Col xs={24} sm={12}>
                          <div style={{ marginBottom: 16 }}>
                            <Text strong>Email:</Text>
                            <br />
                            <Text>{user?.email}</Text>
                          </div>
                        </Col>
                      </Row>
                      
                      <Row gutter={16}>
                        <Col xs={24} sm={12}>
                          <div style={{ marginBottom: 16 }}>
                            <Text strong>Số Điện Thoại:</Text>
                            <br />
                            <Text>{user?.phone || 'Chưa cập nhật'}</Text>
                          </div>
                        </Col>
                        <Col xs={24} sm={12}>
                          <div style={{ marginBottom: 16 }}>
                            <Text strong>Phòng Ban:</Text>
                            <br />
                            <Text>{user?.department || 'Phòng Công Nghệ Thông Tin'}</Text>
                          </div>
                        </Col>
                      </Row>
                    </Card>
                  </Col>
                  
                  <Col span={24}>
                    <Card size="small" title="Thông Tin Công Việc">
                      <div style={{ marginBottom: 16 }}>
                        <Text strong>Chức Vụ:</Text>
                        <br />
                        <Text>{user?.position || 'Quản Trị Viên Hệ Thống'}</Text>
                      </div>
                      
                      <div>
                        <Text strong>Giới Thiệu:</Text>
                        <br />
                        <Text>{user?.bio || 'Quản trị viên hệ thống chatbot TNUT, phụ trách vận hành và bảo trì hệ thống hỗ trợ học tập cho sinh viên.'}</Text>
                      </div>
                    </Card>
                  </Col>
                </Row>
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Profile;
