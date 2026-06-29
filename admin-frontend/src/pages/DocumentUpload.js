import React, { useState } from 'react';
import { 
  Card, 
  Upload, 
  Button, 
  Table, 
  message, 
  Progress, 
  Typography, 
  Space,
  Tag,
  Popconfirm,
  Row,
  Col,
  Statistic,
  Modal,
  Form,
  Input,
  Select
} from 'antd';
import { 
  UploadOutlined, 
  FileTextOutlined, 
  DeleteOutlined,
  EyeOutlined,
  CloudUploadOutlined,
  FileOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined
} from '@ant-design/icons';

const { Title, Text } = Typography;
const { Dragger } = Upload;
const { TextArea } = Input;

const DocumentUpload = () => {
  const [uploading, setUploading] = useState(false);
  const [documents, setDocuments] = useState([]);
  const [fileList, setFileList] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [form] = Form.useForm();

  // Mock existing documents
  const mockDocuments = [
    {
      id: 1,
      name: 'Giáo trình Toán Cao Cấp A1.pdf',
      size: '2.5 MB',
      type: 'pdf',
      status: 'processed',
      uploadDate: '2024-08-20 10:30',
      category: 'Toán học',
      description: 'Giáo trình toán cao cấp dành cho sinh viên năm nhất',
      vectorCount: 1250
    },
    {
      id: 2,
      name: 'Bài giảng Vật lý đại cương.docx',
      size: '1.8 MB',
      type: 'docx',
      status: 'processing',
      uploadDate: '2024-08-25 09:15',
      category: 'Vật lý',
      description: 'Bài giảng vật lý đại cương học kỳ 1',
      vectorCount: 0
    },
    {
      id: 3,
      name: 'Hướng dẫn thực hành Lập trình C++.pdf',
      size: '3.2 MB',
      type: 'pdf',
      status: 'failed',
      uploadDate: '2024-08-24 14:20',
      category: 'Tin học',
      description: 'Hướng dẫn thực hành lập trình C++ cơ bản',
      vectorCount: 0
    },
    {
      id: 4,
      name: 'Tài liệu Tiếng Anh chuyên ngành.pdf',
      size: '4.1 MB',
      type: 'pdf',
      status: 'processed',
      uploadDate: '2024-08-23 16:45',
      category: 'Ngoại ngữ',
      description: 'Tài liệu tiếng Anh chuyên ngành kỹ thuật',
      vectorCount: 2100
    }
  ];

  React.useEffect(() => {
    setDocuments(mockDocuments);
  }, []);

  const uploadProps = {
    name: 'file',
    multiple: true,
    fileList,
    beforeUpload: (file) => {
      const isPDF = file.type === 'application/pdf';
      const isDoc = file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' ||
                    file.type === 'application/msword';
      const isTxt = file.type === 'text/plain';
      
      if (!isPDF && !isDoc && !isTxt) {
        message.error('Chỉ hỗ trợ file PDF, Word và TXT!');
        return false;
      }
      
      const isLt10M = file.size / 1024 / 1024 < 10;
      if (!isLt10M) {
        message.error('File phải nhỏ hơn 10MB!');
        return false;
      }
      
      return false; // Prevent auto upload
    },
    onChange: (info) => {
      setFileList(info.fileList);
    },
    onDrop(e) {
      console.log('Dropped files', e.dataTransfer.files);
    },
  };

  const handleUpload = async () => {
    if (fileList.length === 0) {
      message.warning('Vui lòng chọn file để tải lên!');
      return;
    }

    setUploading(true);
    
    // Simulate upload process
    setTimeout(() => {
      const newDocuments = fileList.map((file, index) => ({
        id: Date.now() + index,
        name: file.name,
        size: (file.size / 1024 / 1024).toFixed(1) + ' MB',
        type: file.name.split('.').pop(),
        status: 'processing',
        uploadDate: new Date().toLocaleString('vi-VN'),
        category: 'Chưa phân loại',
        description: 'Đang xử lý...',
        vectorCount: 0
      }));
      
      setDocuments([...newDocuments, ...documents]);
      setFileList([]);
      setUploading(false);
      message.success('Tải lên thành công! Đang xử lý tài liệu...');
    }, 2000);
  };

  const handleDelete = (id) => {
    setDocuments(documents.filter(doc => doc.id !== id));
    message.success('Xóa tài liệu thành công!');
  };

  const handleAddMetadata = (record) => {
    form.setFieldsValue({
      name: record.name,
      category: record.category,
      description: record.description
    });
    setModalVisible(true);
  };

  const handleMetadataSubmit = async () => {
    try {
      const values = await form.validateFields();
      // Update document metadata
      message.success('Cập nhật thông tin thành công!');
      setModalVisible(false);
    } catch (error) {
      console.error('Validation failed:', error);
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      processed: 'success',
      processing: 'processing',
      failed: 'error'
    };
    return colors[status] || 'default';
  };

  const getStatusText = (status) => {
    const texts = {
      processed: 'Đã xử lý',
      processing: 'Đang xử lý',
      failed: 'Thất bại'
    };
    return texts[status] || status;
  };

  const getFileIcon = (type) => {
    switch (type) {
      case 'pdf':
        return <FileTextOutlined style={{ color: '#ff4d4f' }} />;
      case 'docx':
      case 'doc':
        return <FileOutlined style={{ color: '#1890ff' }} />;
      default:
        return <FileOutlined />;
    }
  };

  const columns = [
    {
      title: 'Tên File',
      dataIndex: 'name',
      key: 'name',
      render: (name, record) => (
        <Space>
          {getFileIcon(record.type)}
          <span>{name}</span>
        </Space>
      ),
    },
    {
      title: 'Kích Thước',
      dataIndex: 'size',
      key: 'size',
      width: 100,
    },
    {
      title: 'Danh Mục',
      dataIndex: 'category',
      key: 'category',
      width: 120,
      render: (category) => <Tag>{category}</Tag>
    },
    {
      title: 'Trạng Thái',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status) => (
        <Tag color={getStatusColor(status)} icon={
          status === 'processed' ? <CheckCircleOutlined /> :
          status === 'processing' ? <SyncOutlined spin /> :
          <CloseCircleOutlined />
        }>
          {getStatusText(status)}
        </Tag>
      ),
    },
    {
      title: 'Vector Count',
      dataIndex: 'vectorCount',
      key: 'vectorCount',
      width: 100,
      render: (count) => count > 0 ? count.toLocaleString() : '-'
    },
    {
      title: 'Ngày Tải Lên',
      dataIndex: 'uploadDate',
      key: 'uploadDate',
      width: 150,
    },
    {
      title: 'Hành Động',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space size="middle">
          <Button 
            type="primary" 
            size="small" 
            icon={<EyeOutlined />}
            onClick={() => handleAddMetadata(record)}
          >
            Chi tiết
          </Button>
          <Popconfirm
            title="Bạn có chắc chắn muốn xóa tài liệu này?"
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

  const stats = [
    {
      title: 'Tổng Tài Liệu',
      value: documents.length,
      prefix: <FileTextOutlined style={{ color: '#1890ff' }} />,
    },
    {
      title: 'Đã Xử Lý',
      value: documents.filter(d => d.status === 'processed').length,
      prefix: <CheckCircleOutlined style={{ color: '#52c41a' }} />,
    },
    {
      title: 'Đang Xử Lý',
      value: documents.filter(d => d.status === 'processing').length,
      prefix: <SyncOutlined style={{ color: '#1890ff' }} />,
    },
    {
      title: 'Thất Bại',
      value: documents.filter(d => d.status === 'failed').length,
      prefix: <CloseCircleOutlined style={{ color: '#ff4d4f' }} />,
    },
  ];

  return (
    <div>
      <Title level={2}>Quản Lý Tài Liệu</Title>

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

      {/* Upload Section */}
      <Card title="Tải Lên Tài Liệu Mới" style={{ marginBottom: 24 }}>
        <Dragger {...uploadProps} style={{ marginBottom: 16 }}>
          <p className="ant-upload-drag-icon">
            <CloudUploadOutlined style={{ fontSize: 48, color: '#1890ff' }} />
          </p>
          <p className="ant-upload-text">
            Kéo thả file vào đây hoặc click để chọn file
          </p>
          <p className="ant-upload-hint">
            Hỗ trợ file PDF, Word và TXT. Kích thước tối đa 10MB mỗi file.
          </p>
        </Dragger>
        
        <Space>
          <Button 
            type="primary" 
            onClick={handleUpload} 
            loading={uploading}
            disabled={fileList.length === 0}
            icon={<UploadOutlined />}
          >
            {uploading ? 'Đang tải lên...' : 'Tải lên'}
          </Button>
          <Text type="secondary">
            {fileList.length} file được chọn
          </Text>
        </Space>
      </Card>

      {/* Documents Table */}
      <Card title="Danh Sách Tài Liệu">
        <Table
          columns={columns}
          dataSource={documents}
          rowKey="id"
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => 
              `${range[0]}-${range[1]} của ${total} tài liệu`,
          }}
        />
      </Card>

      {/* Document Details Modal */}
      <Modal
        title="Thông Tin Tài Liệu"
        open={modalVisible}
        onOk={handleMetadataSubmit}
        onCancel={() => setModalVisible(false)}
        okText="Cập Nhật"
        cancelText="Hủy"
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          name="document_form"
        >
          <Form.Item
            name="name"
            label="Tên Tài Liệu"
            rules={[{ required: true, message: 'Vui lòng nhập tên tài liệu!' }]}
          >
            <Input placeholder="Nhập tên tài liệu" />
          </Form.Item>

          <Form.Item
            name="category"
            label="Danh Mục"
            rules={[{ required: true, message: 'Vui lòng chọn danh mục!' }]}
          >
            <Select placeholder="Chọn danh mục">
              <Select.Option value="Toán học">Toán học</Select.Option>
              <Select.Option value="Vật lý">Vật lý</Select.Option>
              <Select.Option value="Tin học">Tin học</Select.Option>
              <Select.Option value="Ngoại ngữ">Ngoại ngữ</Select.Option>
              <Select.Option value="Cơ khí">Cơ khí</Select.Option>
              <Select.Option value="Điện - Điện tử">Điện - Điện tử</Select.Option>
              <Select.Option value="Kinh tế">Kinh tế</Select.Option>
              <Select.Option value="Khác">Khác</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="description"
            label="Mô Tả"
            rules={[{ required: true, message: 'Vui lòng nhập mô tả!' }]}
          >
            <TextArea 
              rows={4} 
              placeholder="Nhập mô tả chi tiết về tài liệu"
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default DocumentUpload;
