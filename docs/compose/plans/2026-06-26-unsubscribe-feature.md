# 取消订阅功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use compose:subagent (recommended) or compose:execute to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在播客应用中添加取消订阅功能，包括单个取消和批量取消订阅。

**Architecture:** 修改 HomeScreen 和 PodcastDetailScreen，添加取消订阅按钮和编辑模式。使用现有的 SubscriptionManager.unsubscribe() 方法。

**Tech Stack:** Kotlin, Compose Multiplatform, Material 3

## Global Constraints

- 使用现有的 SubscriptionManager 类，不修改数据层
- 保持现有的 UI 风格和 Material 3 设计
- 所有文本使用 Strings 资源（支持多语言）
- 取消订阅操作需要确认对话框
- 批量取消订阅需要编辑模式

---

### Task 1: 修改 HomeScreen 添加编辑模式和取消订阅功能

**Covers:** [S3, S4]

**Files:**
- Modify: `desktop/src/desktopMain/kotlin/app/podiumpodcasts/podium/desktop/App.kt:238-309`

**Interfaces:**
- Consumes: `Podcast` 数据模型，`AppDatabase` 实例，`SubscriptionManager` 实例
- Produces: 修改后的 HomeScreen 组件，支持编辑模式和取消订阅

- [ ] **Step 1: 创建 SubscriptionManager 实例并添加状态变量**

在 App.kt 的 App 函数中，创建 SubscriptionManager 实例：

```kotlin
val subscriptionManager = remember { SubscriptionManager(database) }
```

在 HomeScreen 函数中添加以下状态变量：

```kotlin
var isEditing by remember { mutableStateOf(false) }
var selectedPodcasts by remember { mutableStateOf(setOf<String>()) }
var showBatchUnsubscribeDialog by remember { mutableStateOf(false) }
```

- [ ] **Step 2: 修改 TopAppBar**

修改 TopAppBar 以支持编辑模式：

```kotlin
TopAppBar(
    title = { 
        if (isEditing) {
            Text("已选择 ${selectedPodcasts.size} 个")
        } else {
            Text(Strings["home_title"]) 
        }
    },
    navigationIcon = {
        if (isEditing) {
            IconButton(onClick = { 
                isEditing = false
                selectedPodcasts = emptySet()
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "取消")
            }
        }
    },
    actions = {
        if (isEditing) {
            IconButton(onClick = { 
                // 全选逻辑
                if (selectedPodcasts.size == podcasts.size) {
                    selectedPodcasts = emptySet()
                } else {
                    selectedPodcasts = podcasts.map { it.origin }.toSet()
                }
            }) {
                Icon(Icons.Default.SelectAll, contentDescription = "全选")
            }
            IconButton(onClick = { showBatchUnsubscribeDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "删除选中")
            }
        } else {
            IconButton(onClick = onDiscover) {
                Icon(Icons.Default.Explore, contentDescription = Strings["nav_discover"])
            }
            IconButton(onClick = onHistory) {
                Icon(Icons.Default.History, contentDescription = Strings["nav_history"])
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = Strings["nav_settings"])
            }
            IconButton(onClick = onAddPodcast) {
                Icon(Icons.Default.Add, contentDescription = Strings["home_add_podcast"])
            }
            IconButton(onClick = { isEditing = true }) {
                Icon(Icons.Default.Edit, contentDescription = "编辑")
            }
        }
    }
)
```

- [ ] **Step 3: 修改播客列表项**

修改播客列表项以支持编辑模式和取消订阅：

```kotlin
items(podcasts) { podcast ->
    ListItem(
        headlineContent = { Text(podcast.title) },
        supportingContent = { Text(podcast.author) },
        leadingContent = {
            if (isEditing) {
                Checkbox(
                    checked = podcast.origin in selectedPodcasts,
                    onCheckedChange = { checked ->
                        selectedPodcasts = if (checked) {
                            selectedPodcasts + podcast.origin
                        } else {
                            selectedPodcasts - podcast.origin
                        }
                    }
                )
            } else {
                AsyncImage(
                    model = podcast.imageUrl,
                    contentDescription = podcast.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        },
        trailingContent = {
            if (!isEditing) {
                IconButton(onClick = { 
                    podcastToUnsubscribe = podcast
                    showUnsubscribeDialog = true
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "取消订阅")
                }
            }
        },
        modifier = Modifier.clickable { 
            if (!isEditing) {
                onPodcastClick(podcast) 
            }
        }
    )
    HorizontalDivider()
}
```

- [ ] **Step 4: 添加取消订阅确认对话框**

在 HomeScreen 中添加取消订阅确认对话框：

```kotlin
// 在 HomeScreen 函数中添加状态变量
var showUnsubscribeDialog by remember { mutableStateOf(false) }
var podcastToUnsubscribe by remember { mutableStateOf<Podcast?>(null) }

// 在 Scaffold 之后添加对话框
if (showUnsubscribeDialog && podcastToUnsubscribe != null) {
    AlertDialog(
        onDismissRequest = { 
            showUnsubscribeDialog = false
            podcastToUnsubscribe = null
        },
        title = { Text("取消订阅") },
        text = { Text("确定要取消订阅 \"${podcastToUnsubscribe!!.title}\" 吗？") },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    subscriptionManager.unsubscribe(podcastToUnsubscribe!!.origin)
                    podcasts = database.podcasts.getAllSync()
                }
                showUnsubscribeDialog = false
                podcastToUnsubscribe = null
            }) {
                Text("取消订阅")
            }
        },
        dismissButton = {
            TextButton(onClick = { 
                showUnsubscribeDialog = false
                podcastToUnsubscribe = null
            }) {
                Text(Strings["dialog_cancel"])
            }
        }
    )
}
```

- [ ] **Step 5: 添加批量取消订阅确认对话框**

在 HomeScreen 中添加批量取消订阅确认对话框：

```kotlin
if (showBatchUnsubscribeDialog) {
    AlertDialog(
        onDismissRequest = { showBatchUnsubscribeDialog = false },
        title = { Text("批量取消订阅") },
        text = { Text("确定要取消订阅选中的 ${selectedPodcasts.size} 个播客吗？") },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    selectedPodcasts.forEach { origin ->
                        subscriptionManager.unsubscribe(origin)
                    }
                    podcasts = database.podcasts.getAllSync()
                    selectedPodcasts = emptySet()
                    isEditing = false
                }
                showBatchUnsubscribeDialog = false
            }) {
                Text("取消订阅")
            }
        },
        dismissButton = {
            TextButton(onClick = { showBatchUnsubscribeDialog = false }) {
                Text(Strings["dialog_cancel"])
            }
        }
    )
}
```

- [ ] **Step 6: 修改取消订阅按钮的点击事件**

修改播客列表项中取消订阅按钮的点击事件：

```kotlin
trailingContent = {
    if (!isEditing) {
        IconButton(onClick = { 
            podcastToUnsubscribe = podcast
            showUnsubscribeDialog = true
        }) {
            Icon(Icons.Default.Delete, contentDescription = "取消订阅")
        }
    }
},
```

- [ ] **Step 7: 运行测试验证**

运行现有的 UI 测试，确保没有破坏现有功能：

Run: `gradlew.bat :desktop:desktopTest`
Expected: 所有现有测试通过

- [ ] **Step 8: 提交更改**

```bash
git add desktop/src/desktopMain/kotlin/app/podiumpodcasts/podium/desktop/App.kt
git commit -m "feat: add edit mode and unsubscribe functionality to HomeScreen"
```

### Task 2: 修改 PodcastDetailScreen 添加取消订阅功能

**Covers:** [S3, S4]

**Files:**
- Modify: `desktop/src/desktopMain/kotlin/app/podiumpodcasts/podium/desktop/App.kt:311-460`

**Interfaces:**
- Consumes: `Podcast` 数据模型，`AppDatabase` 实例，`SubscriptionManager` 实例
- Produces: 修改后的 PodcastDetailScreen 组件，支持取消订阅

- [ ] **Step 1: 添加状态变量**

在 PodcastDetailScreen 函数中添加以下状态变量：

```kotlin
var showUnsubscribeDialog by remember { mutableStateOf(false) }
```

- [ ] **Step 2: 修改 TopAppBar**

修改 TopAppBar 以添加取消订阅按钮：

```kotlin
Scaffold(
    topBar = {
        TopAppBar(
            title = { Text(podcast.title) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = Strings["nav_back"])
                }
            },
            actions = {
                IconButton(onClick = { showUnsubscribeDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "取消订阅")
                }
            }
        )
    }
) { padding -> ... }
```

- [ ] **Step 3: 添加取消订阅确认对话框**

在 PodcastDetailScreen 中添加取消订阅确认对话框：

```kotlin
if (showUnsubscribeDialog) {
    AlertDialog(
        onDismissRequest = { showUnsubscribeDialog = false },
        title = { Text("取消订阅") },
        text = { Text("确定要取消订阅 \"${podcast.title}\" 吗？") },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    subscriptionManager.unsubscribe(podcast.origin)
                    onBack()
                }
                showUnsubscribeDialog = false
            }) {
                Text("取消订阅")
            }
        },
        dismissButton = {
            TextButton(onClick = { showUnsubscribeDialog = false }) {
                Text(Strings["dialog_cancel"])
            }
        }
    )
}
```

- [ ] **Step 4: 运行测试验证**

运行现有的 UI 测试，确保没有破坏现有功能：

Run: `gradlew.bat :desktop:desktopTest`
Expected: 所有现有测试通过

- [ ] **Step 5: 提交更改**

```bash
git add desktop/src/desktopMain/kotlin/app/podiumpodcasts/podium/desktop/App.kt
git commit -m "feat: add unsubscribe functionality to PodcastDetailScreen"
```

### Task 3: 添加必要的字符串资源

**Covers:** [S3]

**Files:**
- Modify: 项目中的字符串资源文件（需要找到具体位置）

**Interfaces:**
- Consumes: 无
- Produces: 新的字符串资源

- [ ] **Step 1: 查找字符串资源文件**

首先查找字符串资源文件的位置：

```bash
find . -name "*.xml" -o -name "*.kt" | xargs grep -l "home_title\|dialog_cancel" | head -5
```

或者使用 CodeGraph 搜索 Strings 相关的代码。

- [ ] **Step 2: 添加新的字符串资源**

在字符串资源文件中添加以下资源：

```xml
<resources>
    <!-- 添加到现有的 resources 文件中 -->
    <string name="home_edit">编辑</string>
    <string name="home_cancel">取消</string>
    <string name="home_select_all">全选</string>
    <string name="home_delete_selected">删除选中</string>
    <string name="home_selected_count">已选择 %d 个</string>
    <string name="unsubscribe">取消订阅</string>
    <string name="unsubscribe_confirm">确定要取消订阅 \"%s\" 吗？</string>
    <string name="batch_unsubscribe">批量取消订阅</string>
    <string name="batch_unsubscribe_confirm">确定要取消订阅选中的 %d 个播客吗？</string>
</resources>
```

- [ ] **Step 3: 更新代码中的字符串引用**

更新 Task 1 和 Task 2 中硬编码的字符串，使用 Strings 资源：

```kotlin
// 将 "编辑" 替换为 Strings["home_edit"]
// 将 "取消" 替换为 Strings["home_cancel"]
// 以此类推
```

- [ ] **Step 4: 运行测试验证**

运行现有的 UI 测试，确保没有破坏现有功能：

Run: `gradlew.bat :desktop:desktopTest`
Expected: 所有现有测试通过

- [ ] **Step 5: 提交更改**

```bash
git add [字符串资源文件路径]
git commit -m "feat: add string resources for unsubscribe functionality"
```

### Task 4: 添加单元测试

**Covers:** [S6]

**Files:**
- Create: `desktop/src/desktopTest/kotlin/app/podiumpodcasts/podium/desktop/UnsubscribeTest.kt`

**Interfaces:**
- Consumes: `SubscriptionManager` 类
- Produces: 单元测试

- [ ] **Step 1: 创建测试文件**

创建新的测试文件：

```kotlin
package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.manager.SubscriptionManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class UnsubscribeTest {
    private lateinit var database: AppDatabase
    private lateinit var subscriptionManager: SubscriptionManager

    @Before
    fun setup() {
        // 创建内存数据库用于测试
        database = AppDatabase.build(File(System.getProperty("java.io.tmpdir"), "test.db"))
        subscriptionManager = SubscriptionManager(database)
    }

    @Test
    fun `test unsubscribe removes subscription`() = runBlocking {
        // 先添加一个订阅
        val origin = "https://example.com/feed.xml"
        subscriptionManager.subscribe(origin)
        
        // 验证已订阅
        assertTrue(subscriptionManager.isSubscribed(origin))
        
        // 取消订阅
        subscriptionManager.unsubscribe(origin)
        
        // 验证已取消订阅
        assertFalse(subscriptionManager.isSubscribed(origin))
    }

    @Test
    fun `test unsubscribe non-existent subscription does not throw`() = runBlocking {
        // 尝试取消一个不存在的订阅
        val origin = "https://nonexistent.com/feed.xml"
        
        // 应该不抛出异常
        subscriptionManager.unsubscribe(origin)
        
        // 验证仍然不存在
        assertFalse(subscriptionManager.isSubscribed(origin))
    }
}
```

- [ ] **Step 2: 运行测试验证**

运行新添加的单元测试：

Run: `gradlew.bat :desktop:desktopTest --tests "app.podiumpodcasts.podium.desktop.UnsubscribeTest"`
Expected: 所有测试通过

- [ ] **Step 3: 提交更改**

```bash
git add desktop/src/desktopTest/kotlin/app/podiumpodcasts/podium/desktop/UnsubscribeTest.kt
git commit -m "test: add unit tests for unsubscribe functionality"
```

### Task 5: 运行完整测试套件并最终验证

**Covers:** [S6]

**Files:**
- 无新文件

**Interfaces:**
- Consumes: 所有之前的任务
- Produces: 验证所有功能正常工作

- [ ] **Step 1: 运行完整测试套件**

运行所有测试，确保没有破坏现有功能：

Run: `gradlew.bat :desktop:desktopTest`
Expected: 所有测试通过

- [ ] **Step 2: 运行 lint 检查**

运行代码质量检查：

Run: `gradlew.bat :desktop:lint`
Expected: 没有新的 lint 错误

- [ ] **Step 3: 构建项目**

构建项目确保可以正常编译：

Run: `gradlew.bat :desktop:build`
Expected: 构建成功

- [ ] **Step 4: 最终提交**

```bash
git add .
git commit -m "feat: complete unsubscribe functionality with tests and validation"
```