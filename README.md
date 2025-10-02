# 🔍 Module Analyzer Maven Plugin

モジュラモノリス構成のプロジェクトを分析するためのMavenプラグインです。

## ✨ 機能

このプラグインは、モジュラモノリスプロジェクトの構造を可視化する2つのコマンドを提供します：

| コマンド | 説明 |
|---------|------|
| 📋 `list-expose` | 各モジュールの公開API（exposeディレクトリ配下のクラス）を一覧表示 |
| 🗄️ `list-table` | 各モジュールのテーブル一覧（Repositoryから推測）を表示 |

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
各モジュールの公開API（`expose` ディレクトリ配下のJavaクラス）を一覧表示します。オプションでモジュール間の依存関係も表示できます。

**注意事項:**
- `*Dto.java` は自動的に除外されます
- exposeクラスがないモジュールでも、依存関係がある場合は表示されます（applicationクラスを含む）

### 実行コマンド
```bash
mvn module-analyzer:list-expose -DrootDir=modules
```

### パラメータ
- `rootDir`: モジュールのルートディレクトリパス（必須）
- `showDependency`: モジュール間の依存関係を表示（デフォルト: false）

### プロジェクト構成例
```
modules/
├── notification/
│   ├── expose/
│   │   ├── SendNotificationApi.java
│   │   └── NotificationDto.java  # Dtoは自動除外
│   ├── application/
│   │   └── SendNotificationCommand.java
│   └── infra/
│       └── NotificationRepository.java
├── order/
│   ├── application/
│   │   └── OrderCommand.java  # 他モジュールのAPIを使用
│   └── infra/
│       └── OrderRepository.java
├── user/
│   ├── expose/
│   │   ├── FindUserApi.java
│   │   └── UserDto.java  # Dtoは自動除外
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
[INFO]   Depended by:
[INFO]     - order: OrderCommand
[INFO]     - user: SignupCommand
[INFO]
[INFO] [Module: order]
[INFO]   - com.example.order.application.OrderCommand
[INFO]   Dependencies to:
[INFO]     - notification: SendNotificationApi
[INFO]     - product: FindProductApi
[INFO]     - user: FindUserApi
[INFO]
[INFO] [Module: product]
[INFO]   - com.example.product.expose.FindProductApi
[INFO]   Depended by:
[INFO]     - order: OrderCommand
[INFO]
[INFO] [Module: user]
[INFO]   - com.example.user.expose.FindUserApi
[INFO]   Dependencies to:
[INFO]     - notification: SendNotificationApi
[INFO]   Depended by:
[INFO]     - order: OrderCommand
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
[INFO]   - order (OrderRepository)
[INFO]   - order_item (OrderItemRepository)
[INFO]
[INFO] [Module: product]
[INFO]   - product (ProductRepository)
[INFO]
[INFO] [Module: user]
[INFO]   - user (UserRepository)
[INFO]   - user_profile (UserProfileRepository)
[INFO]   - user_setting (UserSettingRepository)
```
