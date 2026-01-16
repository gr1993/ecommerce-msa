import { useState, useEffect, useCallback } from 'react'
import { Tree, Form, Input, InputNumber, Button, Space, message, Card, Switch, Spin } from 'antd'
import type { DataNode } from 'antd/es/tree'
import { PlusOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons'
import { getCategoryTree, getCategory, createCategory, type CategoryTreeResponse } from '../../../api/categoryApi'
import './AdminCategoryManage.css'

const ROOT_KEY = 'root'

interface CategoryNode {
  key: string
  title: string
  displayOrder: number
  isDisplayed: boolean
  children?: CategoryNode[]
  depth: number
  categoryId?: number
}

function AdminCategoryManage() {
  const [form] = Form.useForm()
  const [selectedKey, setSelectedKey] = useState<string | null>(ROOT_KEY)
  const [treeData, setTreeData] = useState<CategoryNode[]>([])
  const [isEditing, setIsEditing] = useState(false)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)

  // API 응답을 CategoryNode로 변환
  const convertApiResponseToNode = (apiNode: CategoryTreeResponse): CategoryNode => {
    return {
      key: `cat_${apiNode.categoryId}`,
      categoryId: apiNode.categoryId,
      title: apiNode.categoryName,
      displayOrder: apiNode.displayOrder,
      isDisplayed: apiNode.isDisplayed,
      depth: apiNode.depth,
      children: apiNode.children?.map(convertApiResponseToNode) || []
    }
  }

  // 카테고리 트리 조회
  const fetchCategoryTree = useCallback(async () => {
    setLoading(true)
    try {
      const data = await getCategoryTree()
      const converted = data.map(convertApiResponseToNode)
      setTreeData(converted)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '카테고리 트리 조회 실패')
    } finally {
      setLoading(false)
    }
  }, [])

  // 초기 로드
  useEffect(() => {
    fetchCategoryTree()
  }, [fetchCategoryTree])

  // 카테고리 노드를 DataNode로 변환 (재귀, display_order로 정렬)
  const convertNodeToDataNode = (node: CategoryNode): DataNode => {
    const sortedChildren = node.children 
      ? [...node.children].sort((a, b) => a.displayOrder - b.displayOrder).map(convertNodeToDataNode)
      : undefined
    
    return {
      key: node.key,
      title: `${node.title} (순서: ${node.displayOrder})`,
      children: sortedChildren
    }
  }

  // 카테고리 트리 데이터를 Ant Design Tree 형식으로 변환 (최상위 루트 포함, display_order로 정렬)
  const convertToTreeData = (nodes: CategoryNode[]): DataNode[] => {
    const sortedNodes = [...nodes].sort((a, b) => a.displayOrder - b.displayOrder)
    return [
      {
        key: ROOT_KEY,
        title: '최상위',
        children: sortedNodes.map(convertNodeToDataNode)
      }
    ]
  }

  // 특정 키의 노드 찾기
  const findNode = (nodes: CategoryNode[], key: string): CategoryNode | null => {
    for (const node of nodes) {
      if (node.key === key) return node
      if (node.children) {
        const found = findNode(node.children, key)
        if (found) return found
      }
    }
    return null
  }

  // 노드 추가
  const addNode = (nodes: CategoryNode[], parentKey: string | null, newNode: CategoryNode): CategoryNode[] => {
    if (parentKey === null) {
      // 루트 레벨에 추가
      return [...nodes, newNode]
    }

    return nodes.map(node => {
      if (node.key === parentKey) {
        // 현재 노드가 부모인 경우
        if (node.depth >= 3) {
          message.error('최대 3단계까지만 생성할 수 있습니다.')
          return node
        }
        return {
          ...node,
          children: [...(node.children || []), newNode]
        }
      }
      if (node.children) {
        return {
          ...node,
          children: addNode(node.children, parentKey, newNode)
        }
      }
      return node
    })
  }

  // 노드 업데이트
  const updateNode = (nodes: CategoryNode[], key: string, updates: Partial<CategoryNode>): CategoryNode[] => {
    return nodes.map(node => {
      if (node.key === key) {
        return { ...node, ...updates }
      }
      if (node.children) {
        return {
          ...node,
          children: updateNode(node.children, key, updates)
        }
      }
      return node
    })
  }

  // 노드 삭제
  const removeNode = (nodes: CategoryNode[], key: string): CategoryNode[] => {
    return nodes
      .filter(node => node.key !== key)
      .map(node => ({
        ...node,
        children: node.children ? removeNode(node.children, key) : undefined
      }))
  }

  // 트리 노드 선택
  const handleSelect = async (selectedKeys: React.Key[]) => {
    if (selectedKeys.length === 0) {
      setSelectedKey(ROOT_KEY)
      setIsEditing(false)
      form.resetFields()
      return
    }

    const key = selectedKeys[0] as string
    setSelectedKey(key)

    if (key === ROOT_KEY) {
      // 최상위 선택 시 추가 모드
      setIsEditing(false)
      form.resetFields()
      form.setFieldsValue({
        display_order: 0,
        is_displayed: true
      })
    } else {
      // 카테고리 선택 시 수정 모드 - API로 상세 조회
      const node = findNode(treeData, key)
      if (node && node.categoryId) {
        setLoading(true)
        try {
          const categoryDetail = await getCategory(node.categoryId)
          form.setFieldsValue({
            category_name: categoryDetail.categoryName,
            display_order: categoryDetail.displayOrder,
            is_displayed: categoryDetail.isDisplayed
          })
          setIsEditing(true)
        } catch (error) {
          message.error(error instanceof Error ? error.message : '카테고리 상세 조회 실패')
          // 실패 시 로컬 데이터로 폴백
          form.setFieldsValue({
            category_name: node.title,
            display_order: node.displayOrder,
            is_displayed: node.isDisplayed
          })
          setIsEditing(true)
        } finally {
          setLoading(false)
        }
      }
    }
  }

  // 새 카테고리 추가 버튼 클릭
  const handleAdd = () => {
    // 현재 선택된 노드의 자식으로 추가할 수 있도록 추가 모드로 전환
    setIsEditing(false)
    form.resetFields()
    form.setFieldsValue({
      display_order: 0,
      is_displayed: true
    })
    // selectedKey는 그대로 유지 (부모로 사용)
  }

  // 카테고리 저장
  const handleSave = async (values: { category_name: string; display_order: number; is_displayed: boolean }) => {
    if (!values.category_name.trim()) {
      message.error('카테고리명을 입력하세요.')
      return
    }

    if (isEditing && selectedKey && selectedKey !== ROOT_KEY) {
      // 수정 - 현재 API 미구현으로 로컬만 업데이트
      const key: string = selectedKey
      setTreeData(updateNode(treeData, key, {
        title: values.category_name,
        displayOrder: values.display_order || 0,
        isDisplayed: values.is_displayed !== undefined ? values.is_displayed : true
      }))
      message.success('카테고리가 수정되었습니다.')
    } else {
      // 추가 - API 호출
      setSaving(true)
      try {
        let parentId: number | null = null

        if (selectedKey && selectedKey !== ROOT_KEY) {
          // 선택된 카테고리의 자식으로 추가
          const parentNode = findNode(treeData, selectedKey)
          if (!parentNode) {
            message.error('부모 카테고리를 찾을 수 없습니다.')
            setSaving(false)
            return
          }

          if (parentNode.depth >= 3) {
            message.error('최대 3단계까지만 생성할 수 있습니다.')
            setSaving(false)
            return
          }

          parentId = parentNode.categoryId || null
        }

        await createCategory({
          parentId,
          categoryName: values.category_name,
          displayOrder: values.display_order || 0,
          isDisplayed: values.is_displayed !== undefined ? values.is_displayed : true
        })

        message.success('카테고리가 추가되었습니다.')
        form.resetFields()
        form.setFieldsValue({
          display_order: 0,
          is_displayed: true
        })

        // 트리 새로고침
        await fetchCategoryTree()
      } catch (error) {
        message.error(error instanceof Error ? error.message : '카테고리 등록 실패')
      } finally {
        setSaving(false)
      }
    }
  }

  // 전시 순서 변경 시 즉시 반영
  const handleDisplayOrderChange = (value: number | null) => {
    if (!isEditing || !selectedKey || selectedKey === ROOT_KEY || value === null) {
      return
    }
    // 수정 모드일 때만 즉시 반영
    const key: string = selectedKey // 타입 가드 후 명시적 타입 지정
    const updatedTree = updateNode(treeData, key, {
      displayOrder: value
    })
    setTreeData(updatedTree)
  }

  // 카테고리 삭제
  const handleDelete = () => {
    if (!selectedKey || selectedKey === ROOT_KEY) {
      message.warning('삭제할 카테고리를 선택하세요.')
      return
    }

    const node = findNode(treeData, selectedKey)
    if (node && node.children && node.children.length > 0) {
      message.error('하위 카테고리가 있는 경우 삭제할 수 없습니다.')
      return
    }

    setTreeData(removeNode(treeData, selectedKey))
    setSelectedKey(ROOT_KEY)
    setIsEditing(false)
    form.resetFields()
    message.success('카테고리가 삭제되었습니다.')
  }

  return (
    <div className="admin-category-manage">
      <div className="category-manage-container">
        <h2>카테고리 관리</h2>
        <div className="category-manage-content">
          {/* 왼쪽: 카테고리 트리 */}
          <div className="category-tree-section">
            <Card
              title="카테고리 트리"
              extra={
                <Space>
                  <Button
                    icon={<ReloadOutlined />}
                    size="small"
                    onClick={fetchCategoryTree}
                    loading={loading}
                  >
                    새로고침
                  </Button>
                  <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    size="small"
                    onClick={handleAdd}
                  >
                    새 카테고리
                  </Button>
                </Space>
              }
            >
              <Spin spinning={loading}>
                <Tree
                  treeData={convertToTreeData(treeData)}
                  selectedKeys={selectedKey ? [selectedKey] : [ROOT_KEY]}
                  onSelect={handleSelect}
                  defaultExpandAll
                  showLine
                />
              </Spin>
            </Card>
          </div>

          {/* 오른쪽: 카테고리 정보 입력 */}
          <div className="category-form-section">
            <Card title={isEditing ? '카테고리 수정' : '카테고리 추가'}>
              <Form
                form={form}
                layout="vertical"
                onFinish={handleSave}
                initialValues={{
                  display_order: 0,
                  is_displayed: true
                }}
              >
                <Form.Item
                  label="카테고리명"
                  name="category_name"
                  rules={[{ required: true, message: '카테고리명을 입력하세요' }]}
                >
                  <Input placeholder="카테고리명을 입력하세요" maxLength={100} />
                </Form.Item>

                <div className="form-row">
                  <Form.Item
                    label="전시 순서"
                    name="display_order"
                    rules={[
                      { required: true, message: '전시 순서를 입력하세요' },
                      { type: 'number', min: 0, message: '0 이상의 숫자를 입력하세요' }
                    ]}
                  >
                    <InputNumber
                      style={{ width: '100%' }}
                      placeholder="0"
                      min={0}
                      onChange={handleDisplayOrderChange}
                    />
                  </Form.Item>

                  <Form.Item
                    label="전시 여부"
                    name="is_displayed"
                    valuePropName="checked"
                  >
                    <Switch />
                  </Form.Item>
                </div>

                <div className="selected-category-info">
                  <p>
                    <strong>부모 카테고리:</strong>{' '}
                    {selectedKey === ROOT_KEY 
                      ? '없음 (최상위)' 
                      : selectedKey ? findNode(treeData, selectedKey)?.title || '없음' : '없음'}
                  </p>
                  {!isEditing && selectedKey && selectedKey !== ROOT_KEY && (
                    <p>
                      <strong>생성될 깊이:</strong>{' '}
                      {`${(findNode(treeData, selectedKey)?.depth || 0) + 1}단계`}
                      {findNode(treeData, selectedKey)?.depth && 
                       findNode(treeData, selectedKey)!.depth >= 3 && (
                        <span style={{ color: '#ff4d4f', marginLeft: 8 }}>
                          (최대 깊이 도달)
                        </span>
                      )}
                    </p>
                  )}
                  {!isEditing && selectedKey === ROOT_KEY && (
                    <p>
                      <strong>생성될 깊이:</strong> 1단계
                    </p>
                  )}
                  {isEditing && selectedKey && selectedKey !== ROOT_KEY && (
                    <p>
                      <strong>현재 깊이:</strong>{' '}
                      {findNode(treeData, selectedKey)?.depth || 0}단계
                    </p>
                  )}
                </div>

                <Form.Item>
                  <Space>
                    <Button
                      type="primary"
                      htmlType="submit"
                      loading={saving}
                      style={{ backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }}
                    >
                      {isEditing ? '수정하기' : '추가하기'}
                    </Button>
                    {isEditing && (
                      <Button 
                        danger 
                        icon={<DeleteOutlined />}
                        onClick={handleDelete}
                      >
                        삭제
                      </Button>
                    )}
                    <Button onClick={() => {
                      form.resetFields()
                      setSelectedKey(null)
                      setIsEditing(false)
                    }}>
                      취소
                    </Button>
                  </Space>
                </Form.Item>
              </Form>
            </Card>
          </div>
        </div>
      </div>
    </div>
  )
}

export default AdminCategoryManage

