import React, { useState, useEffect } from 'react';
import { 
  Table, 
  Button, 
  Modal, 
  Form, 
  Input, 
  Select, 
  Space, 
  Popconfirm, 
  message, 
  Typography,
  Tag,
  Avatar,
  Card,
  Row,
  Col,
  Statistic
} from 'antd';
import { 
  PlusOutlined, 
  EditOutlined, 
  DeleteOutlined, 
  UserOutlined,
  SearchOutlined,
  ReloadOutlined
} from '@ant-design/icons';

const { Title } = Typography;
const { Search } = Input;

const AccountManagement = () => {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingAccount, setEditingAccount] = useState(null);
  const [searchText, setSearchText] = useState('');
  const [form] = Form.useForm();

  // Mock data
  const mockAccounts = [
    {
      id: 1,
      email: 'student1@tnut.edu.vn',
      name: 'Nguyễn Văn A',
      role: 'student',
      status: 'active',
      createdAt: '2024-01-15',
      lastLogin: '2024-08-25 09:30',
      messageCount: 45
    },
    {
      id: 2,
      email: 'student2@tnut.edu.vn',
      name: 'Trần Thị B',
      role: 'student',
      status: 'active',
      createdAt: '2024-01-20',
      lastLogin: '2024-08-24 14:20',
      messageCount: 23
    },
    {
      id: 3,
      email: 'teacher1@tnut.edu.vn',
      name: 'Phạm Văn C',
      role: 'teacher',
      status: 'inactive',
      createdAt: '2024-02-01',
      lastLogin: '2024-08-20 10:15',
      messageCount: 78
    },
    {
      id: 4,
      email: 'student3@tnut.edu.vn',
      name: 'Lê Thị D',
      role: 'student',
      status: 'active',
      createdAt: '2024-02-10',
      lastLogin: '2024-08-25 11:45',
      messageCount: 12
    },
    {
      id: 5,
      email: 'teacher2@tnut.edu.vn',
      name: 'Hoàng Văn E',
      role: 'teacher',
      status: 'active',
      createdAt: '2024-02-15',
      lastLogin: '2024-08-25 08:00',
      messageCount: 156
    }
  ];

  useEffect(() => {
    loadAccounts();
  }, []);

  const loadAccounts = async () => {
    setLoading(true);
    // Simulate API call
    setTimeout(() => {
      setAccounts(mockAccounts);
      setLoading(false);
    }, 1000);
  };

  const handleAdd = () => {
    setEditingAccount(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record) => {
    setEditingAccount(record);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  const handleDelete = async (id) => {
    setAccounts(accounts.filter(account => account.id !== id));
    message.success('Xóa tài khoản thành công!');
  };

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();
      
      if (editingAccount) {
        // Update existing account
        setAccounts(accounts.map(account => 
          account.id === editingAccount.id 
            ? { ...account, ...values }
            : account
        ));
        message.success('Cập nhật tài khoản thành công!');
      } else {
        // Add new account
        const newAccount = {
          id: Date.now(),
          ...values,
          createdAt: new Date().toISOString().split('T')[0],
          lastLogin: '-',
          messageCount: 0
        };
        setAccounts([...accounts, newAccount]);
        message.success('Thêm tài khoản thành công!');
      }
      
      setModalVisible(false);
    } catch (error) {
      console.error('Validation failed:', error);
    }
  };

  const columns = [
    {
      title: 'Avatar',
      dataIndex: 'avatar',
      key: 'avatar',
      width: 80,
      render: (_, record) => (
        <Avatar size="large" icon={<UserOutlined />} />
      ),
    },
    {
      title: 'Tên',
      dataIndex: 'name',
      key: 'name',
      sorter: (a, b) => a.name.localeCompare(b.name),
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: 'Vai Trò',
      dataIndex: 'role',
      key: 'role',
      render: (role) => (
        <Tag color={role === 'teacher' ? 'blue' : 'green'}>
          {role === 'teacher' ? 'Giảng Viên' : 'Sinh Viên'}
        </Tag>
      ),
      filters: [
        { text: 'Sinh Viên', value: 'student' },
        { text: 'Giảng Viên', value: 'teacher' },
      ],
      onFilter: (value, record) => record.role === value,
    },
    {
      title: 'Trạng Thái',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={status === 'active' ? 'success' : 'default'}>
          {status === 'active' ? 'Hoạt Động' : 'Không Hoạt Động'}
        </Tag>
      ),
      filters: [
        { text: 'Hoạt Động', value: 'active' },
        { text: 'Không Hoạt Động', value: 'inactive' },
      ],
      onFilter: (value, record) => record.status === value,
    },
    {
      title: 'Số Tin Nhắn',
      dataIndex: 'messageCount',
      key: 'messageCount',
      sorter: (a, b) => a.messageCount - b.messageCount,
    },
    {
      title: 'Lần Đăng Nhập Cuối',
      dataIndex: 'lastLogin',
      key: 'lastLogin',
    },
    {
      title: 'Hành Động',
      key: 'action',
      width: 120,
      render: (_, record) => (
        <Space size="middle">
          <Button 
            type="primary" 
            size="small" 
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          />
          <Popconfirm
            title="Bạn có chắc chắn muốn xóa tài khoản này?"
            onConfirm={() => handleDelete(record.id)}
            okText="Có"
            cancelText="Không"
          >
            <Button 
              type="primary" 
              danger 
              size="small" 
              icon={<DeleteOutlined />}
            />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const filteredAccounts = accounts.filter(account => 
    account.name.toLowerCase().includes(searchText.toLowerCase()) ||
    account.email.toLowerCase().includes(searchText.toLowerCase())
  );

  const stats = [
    {
      title: 'Tổng Tài Khoản',
      value: accounts.length,
      prefix: <UserOutlined style={{ color: '#1890ff' }} />,
    },
    {
      title: 'Sinh Viên',
      value: accounts.filter(a => a.role === 'student').length,
      prefix: <UserOutlined style={{ color: '#52c41a' }} />,
    },
    {
      title: 'Giảng Viên',
      value: accounts.filter(a => a.role === 'teacher').length,
      prefix: <UserOutlined style={{ color: '#722ed1' }} />,
    },
    {
      title: 'Đang Hoạt Động',
      value: accounts.filter(a => a.status === 'active').length,
      prefix: <UserOutlined style={{ color: '#f5222d' }} />,
    },
  ];

  return (
    <div>
      <Title level={2}>Quản Lý Tài Khoản</Title>

      {/* Statistics */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        {stats.map((stat, index) => (
          <Col xs={24} sm={12} lg={6} key={index}>
            <Card>
              <Statistic {...stat} />
            </Card>
          </Col>
        ))}
      </Row>

      {/* Controls */}
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16} align="middle">
          <Col xs={24} sm={8}>
            <Search
              placeholder="Tìm kiếm theo tên hoặc email"
              allowClear
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              prefix={<SearchOutlined />}
            />
          </Col>
          <Col xs={24} sm={16} style={{ textAlign: 'right' }}>
            <Space>
              <Button 
                icon={<ReloadOutlined />}
                onClick={loadAccounts}
                loading={loading}
              >
                Làm Mới
              </Button>
              <Button 
                type="primary" 
                icon={<PlusOutlined />}
                onClick={handleAdd}
              >
                Thêm Tài Khoản
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Table */}
      <Card>
        <Table
          columns={columns}
          dataSource={filteredAccounts}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => 
              `${range[0]}-${range[1]} của ${total} tài khoản`,
          }}
        />
      </Card>

      {/* Add/Edit Modal */}
      <Modal
        title={editingAccount ? 'Sửa Tài Khoản' : 'Thêm Tài Khoản'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        okText={editingAccount ? 'Cập Nhật' : 'Thêm'}
        cancelText="Hủy"
      >
        <Form
          form={form}
          layout="vertical"
          name="account_form"
        >
          <Form.Item
            name="name"
            label="Tên"
            rules={[
              { required: true, message: 'Vui lòng nhập tên!' },
              { min: 2, message: 'Tên phải có ít nhất 2 ký tự!' }
            ]}
          >
            <Input placeholder="Nhập tên" />
          </Form.Item>

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

          <Form.Item
            name="role"
            label="Vai Trò"
            rules={[{ required: true, message: 'Vui lòng chọn vai trò!' }]}
          >
            <Select placeholder="Chọn vai trò">
              <Select.Option value="student">Sinh Viên</Select.Option>
              <Select.Option value="teacher">Giảng Viên</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="status"
            label="Trạng Thái"
            rules={[{ required: true, message: 'Vui lòng chọn trạng thái!' }]}
          >
            <Select placeholder="Chọn trạng thái">
              <Select.Option value="active">Hoạt Động</Select.Option>
              <Select.Option value="inactive">Không Hoạt Động</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default AccountManagement;
