import React from 'react';
import { Row, Col, Card, Statistic, Typography, List, Progress, Tag } from 'antd';
import { 
  UserOutlined, 
  FileTextOutlined, 
  MessageOutlined,
  TrophyOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined
} from '@ant-design/icons';

const { Title, Text } = Typography;

const DashboardHome = () => {
  // Mock data
  const stats = [
    {
      title: 'Tổng Tài Khoản',
      value: 1234,
      precision: 0,
      valueStyle: { color: '#3f8600' },
      prefix: <UserOutlined />,
      suffix: <ArrowUpOutlined style={{ fontSize: '12px' }} />,
    },
    {
      title: 'Tài Liệu Đã Tải',
      value: 89,
      precision: 0,
      valueStyle: { color: '#1890ff' },
      prefix: <FileTextOutlined />,
    },
    {
      title: 'Tin Nhắn Hôm Nay',
      value: 456,
      precision: 0,
      valueStyle: { color: '#cf1322' },
      prefix: <MessageOutlined />,
      suffix: <ArrowUpOutlined style={{ fontSize: '12px' }} />,
    },
    {
      title: 'Điểm Đánh Giá',
      value: 4.8,
      precision: 1,
      valueStyle: { color: '#722ed1' },
      prefix: <TrophyOutlined />,
    },
  ];

  const recentActivities = [
    {
      title: 'Người dùng mới đăng ký',
      description: 'Nguyễn Văn A đã đăng ký tài khoản',
      time: '2 phút trước',
      status: 'success'
    },
    {
      title: 'Tài liệu mới được tải lên',
      description: 'Giáo trình Toán Cao Cấp.pdf',
      time: '5 phút trước',
      status: 'processing'
    },
    {
      title: 'Phản hồi từ người dùng',
      description: 'Chatbot trả lời không chính xác về câu hỏi...',
      time: '10 phút trước',
      status: 'warning'
    },
    {
      title: 'Cập nhật hệ thống',
      description: 'Đã cập nhật phiên bản mới của chatbot',
      time: '1 giờ trước',
      status: 'default'
    },
  ];

  const systemHealth = [
    { name: 'CPU Usage', percent: 45, status: 'success' },
    { name: 'Memory Usage', percent: 67, status: 'normal' },
    { name: 'Disk Usage', percent: 23, status: 'success' },
    { name: 'Network', percent: 89, status: 'exception' },
  ];

  const getStatusColor = (status) => {
    const colors = {
      success: 'green',
      processing: 'blue',
      warning: 'orange',
      default: 'default'
    };
    return colors[status] || 'default';
  };

  return (
    <div>
      <Title level={2} style={{ marginBottom: 24 }}>
        Trang Chủ Dashboard
      </Title>

      {/* Statistics Cards */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        {stats.map((stat, index) => (
          <Col xs={24} sm={12} lg={6} key={index}>
            <Card>
              <Statistic {...stat} />
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={16}>
        {/* Recent Activities */}
        <Col xs={24} lg={12}>
          <Card title="Hoạt Động Gần Đây" style={{ marginBottom: 16 }}>
            <List
              dataSource={recentActivities}
              renderItem={(item) => (
                <List.Item>
                  <List.Item.Meta
                    title={
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span>{item.title}</span>
                        <Tag color={getStatusColor(item.status)}>{item.time}</Tag>
                      </div>
                    }
                    description={item.description}
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>

        {/* System Health */}
        <Col xs={24} lg={12}>
          <Card title="Tình Trạng Hệ Thống" style={{ marginBottom: 16 }}>
            {systemHealth.map((item, index) => (
              <div key={index} style={{ marginBottom: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                  <Text>{item.name}</Text>
                  <Text>{item.percent}%</Text>
                </div>
                <Progress 
                  percent={item.percent} 
                  status={item.status}
                  showInfo={false}
                />
              </div>
            ))}
          </Card>
        </Col>
      </Row>

      {/* Quick Actions */}
      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col span={24}>
          <Card title="Thống Kê Nhanh">
            <Row gutter={16}>
              <Col xs={24} sm={8}>
                <Card size="small" style={{ textAlign: 'center' }}>
                  <Title level={4}>24h</Title>
                  <Text type="secondary">Người dùng trực tuyến</Text>
                  <div style={{ fontSize: '24px', color: '#52c41a', margin: '8px 0' }}>
                    89
                  </div>
                </Card>
              </Col>
              <Col xs={24} sm={8}>
                <Card size="small" style={{ textAlign: 'center' }}>
                  <Title level={4}>7 ngày</Title>
                  <Text type="secondary">Tài khoản mới</Text>
                  <div style={{ fontSize: '24px', color: '#1890ff', margin: '8px 0' }}>
                    156
                  </div>
                </Card>
              </Col>
              <Col xs={24} sm={8}>
                <Card size="small" style={{ textAlign: 'center' }}>
                  <Title level={4}>Tháng này</Title>
                  <Text type="secondary">Tổng tin nhắn</Text>
                  <div style={{ fontSize: '24px', color: '#722ed1', margin: '8px 0' }}>
                    12.8K
                  </div>
                </Card>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default DashboardHome;
