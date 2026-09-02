// ============================================================
// 主布局：侧边栏 + 顶部栏 + 内容区
// ============================================================

import { Layout, Menu, Avatar, Dropdown, Space, theme } from 'antd'
import {
  DashboardOutlined,
  BookOutlined,
  AppstoreOutlined,
  ShoppingCartOutlined,
  UnorderedListOutlined,
  UserOutlined,
  LogoutOutlined,
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useUserStore } from '@/store/userStore'

const { Header, Sider, Content } = Layout

export default function MainLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useUserStore()
  const {
    token: { colorBgContainer },
  } = theme.useToken()

  // 菜单项
  const menuItems = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: '首页',
    },
    {
      key: '/books',
      icon: <BookOutlined />,
      label: '图书管理',
    },
    {
      key: '/categories',
      icon: <AppstoreOutlined />,
      label: '分类管理',
    },
    {
      key: '/my-borrows',
      icon: <ShoppingCartOutlined />,
      label: '我的借阅',
    },
    // 管理员可见
    ...(user?.role === 'ADMIN'
      ? [
          {
            key: '/all-borrows',
            icon: <UnorderedListOutlined />,
            label: '借阅记录',
          },
        ]
      : []),
  ]

  // 用户下拉菜单
  const userMenuItems = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: () => {
        logout()
        navigate('/login')
      },
    },
  ]

  return (
    <Layout className="main-layout">
      {/* 侧边栏 */}
      <Sider width={220} theme="dark">
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontSize: 18,
            fontWeight: 600,
            borderBottom: '1px solid rgba(255,255,255,0.1)',
          }}
        >
          图书管理系统
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ marginTop: 8 }}
        />
      </Sider>

      <Layout>
        {/* 顶部栏 */}
        <Header className="main-header" style={{ background: colorBgContainer }}>
          <div className="header-title">欢迎使用图书管理系统</div>
          <div className="header-user">
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <Space style={{ cursor: 'pointer' }}>
                <Avatar icon={<UserOutlined />} />
                <span>{user?.nickname || user?.username || '用户'}</span>
                <span style={{ color: '#999', fontSize: 12 }}>
                  {user?.role === 'ADMIN' ? '管理员' : '普通用户'}
                </span>
              </Space>
            </Dropdown>
          </div>
        </Header>

        {/* 内容区 */}
        <Content className="main-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
