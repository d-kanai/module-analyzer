# 🔍 Module Analyzer Maven Plugin

モジュラモノリス構成のプロジェクトを分析するためのMavenプラグインです。

## ✨ 機能

このプラグインは、モジュラモノリスプロジェクトの構造を可視化する3つのコマンドを提供します：

| コマンド | 説明 |
|---------|------|
| 📋 `list-expose` | 各モジュールの公開API（exposeディレクトリ配下のクラス）を一覧表示 |
| 🗄️ `list-table` | 各モジュールのテーブル一覧（Repositoryから推測）を表示 |
| 🌐 `list-http-request` | applicationレイヤからのHTTPリクエストを追跡して一覧表示 |

## 🚀 インストール

`pom.xml` に以下を追加：

```xml
<pluginRepositories>
    <pluginRepository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </pluginRepository>
</pluginRepositories>

<build>
    <plugins>
        <plugin>
            <groupId>com.github.d-kanai</groupId>
            <artifactId>module-analyzer</artifactId>
            <version>v1.0.2</version>
        </plugin>
    </plugins>
</build>
```

## 📋 コマンド: list-expose

### 説明
各モジュールの公開API（`expose` ディレクトリ配下のクラス）と依存関係を表示します

### 実行コマンド
```bash
mvn module-analyzer:list-expose -DrootDir=modules
```

### パラメータ
- `rootDir`: モジュールのルートディレクトリパス（必須）
- `showDependency`: クラスごとの依存関係を表示（デフォルト: false）

### プロジェクト構成例
```
modules/
├── notification/
│   ├── expose/
│   │   └── SendNotificationApi.java
│   ├── application/
│   │   └── SendNotificationCommand.java
│   └── infra/
│       └── NotificationRepository.java
├── order/
│   ├── application/
│   │   └── OrderCommand.java
│   └── infra/
│       └── OrderRepository.java
├── user/
│   ├── expose/
│   │   └── FindUserApi.java
│   ├── application/
│   │   └── SignupCommand.java
│   └── infra/
│       └── UserRepository.java
└── product/
    ├── expose/
    │   └── FindProductApi.java
    ├── application/
    │   └── FindProductQuery.java
    └── infra/
        └── ProductRepository.java
```

### 実行結果

#### 基本実行（依存関係なし）
```bash
$ mvn module-analyzer:list-expose -DrootDir=modules

[INFO] [Module: notification]
[INFO]   - com.example.notification.expose.SendNotificationApi
[INFO]
[INFO] [Module: product]
[INFO]   - com.example.product.expose.FindProductApi
[INFO]
[INFO] [Module: user]
[INFO]   - com.example.user.expose.FindUserApi
```

#### showDependency オプション（依存関係を表示）
```bash
$ mvn module-analyzer:list-expose -DrootDir=modules -DshowDependency=true

[INFO] [Module: notification]
[INFO]   - com.example.notification.expose.SendNotificationApi
[INFO]     Depended by:
[INFO]       - order: OrderCommand
[INFO]       - user: SignupCommand
[INFO]
[INFO] [Module: order]
[INFO]   - com.example.order.application.OrderCommand
[INFO]     Dependencies to:
[INFO]       - notification: SendNotificationApi
[INFO]       - product: FindProductApi
[INFO]       - user: FindUserApi
[INFO]
[INFO] [Module: product]
[INFO]   - com.example.product.expose.FindProductApi
[INFO]     Depended by:
[INFO]       - order: OrderCommand
[INFO]
[INFO] [Module: user]
[INFO]   - com.example.user.expose.FindUserApi
[INFO]     Depended by:
[INFO]       - order: OrderCommand
[INFO]   - com.example.user.application.SignupCommand
[INFO]     Dependencies to:
[INFO]       - notification: SendNotificationApi
```

---

## 🗄️ コマンド: list-table

### 説明
各モジュールのテーブル一覧を表示します。`XxxRepository` というクラス名から対応するテーブル名（スネークケース）を推測します。

### 実行コマンド
```bash
mvn module-analyzer:list-table -DrootDir=modules
```

### パラメータ
- `rootDir`: モジュールのルートディレクトリパス（必須）

### プロジェクト構成例
```
modules/
├── order/
│   └── infra/
│       ├── OrderRepository.java
│       └── OrderItemRepository.java
├── user/
│   └── infra/
│       ├── UserRepository.java
│       ├── UserProfileRepository.java
│       └── UserSettingRepository.java
└── product/
    └── infra/
        └── ProductRepository.java
```

### 実行結果
```bash
$ mvn module-analyzer:list-table -DrootDir=modules

[INFO] [Module: order]
[INFO]   - order
[INFO]   - order_item
[INFO]
[INFO] [Module: product]
[INFO]   - product
[INFO]
[INFO] [Module: user]
[INFO]   - user
[INFO]   - user_profile
[INFO]   - user_setting
```

---

## 🌐 コマンド: list-http-request

### 説明
applicationレイヤのメソッドから外部HTTPリクエスト（`client.post`, `client.get`）までの呼び出しを追跡し、どのapplicationレイヤのメソッドがHTTP通信を行っているかを一覧表示します。

### 実行コマンド
```bash
mvn module-analyzer:list-http-request -DrootDir=modules
```

### パラメータ
- `rootDir`: モジュールのルートディレクトリパス（必須）
- `searchPatterns`: 検索するパターン（デフォルト: `client.post,client.get`）

### プロジェクト構成例
```
modules/
├── order/
│   ├── application/
│   │   └── OrderCommand.java         # applicationレイヤ
│   ├── domain/
│   │   └── OrderService.java         # 中間レイヤ
│   └── infra/
│       ├── OrderRepository.java      # infraレイヤ
│       └── Client.java               # HTTPクライアント
└── product/
    ├── application/
    │   └── ProductCommand.java
    └── infra/
        ├── ProductRepository.java
        └── Client.java
```

### 実行結果
```bash
$ mvn module-analyzer:list-http-request -DrootDir=modules

[INFO] Scanning modules in: modules
[INFO] Target subdirectory: application
[INFO] Searching for patterns: client.post, client.get
[INFO]
[INFO] [Module: order]
[INFO]   - OrderCommand.createOrder -> /api/orders
[INFO]   - OrderCommand.placeOrder -> https://orders.example.com/api/orders/save
[INFO]   - OrderCommand.submitOrder -> https://orders.example.com/api/orders/save
[INFO]
[INFO] [Module: product]
[INFO]   - ProductCommand.createProduct -> https://api.example.com/api/products
[INFO]   - ProductCommand.updateProduct -> https://api.example.com/api/products/update
[INFO]   - ProductCommand.getProduct -> https://api.example.com/api/products/123
[INFO]   - ProductCommand.listProducts -> https://api.example.com/api/products
```

### 機能詳細

#### 依存関係の追跡
以下のような複数階層の呼び出しを自動追跡します：

```java
// 1. 直接呼び出し（0階層）
class OrderCommand {
    public void createOrder() {
        client.post("/api/orders", data);  // ← 検出
    }
}

// 2. 1階層経由
class OrderCommand {
    public void placeOrder() {
        orderRepository.save(data);  // → OrderRepository → client.post
    }
}

// 3. 2階層経由
class OrderCommand {
    public void submitOrder() {
        orderService.process(data);  // → OrderService → OrderRepository → client.post
    }
}
```

#### URL解析
変数・定数の連結を自動解決：

```java
private static final String BASE_URL = "https://api.example.com";
private static final String PATH = "/api/products";

public void createProduct() {
    client.post(BASE_URL + PATH, data);
    // → 出力: https://api.example.com/api/products
}
```

#### エラー耐性
パース失敗時もエラーを表示して処理を継続：

```java
client.post(  // 引数が不完全
// → 出力: OrderCommand.brokenMethod -> [Parse Error: ...]
```
