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

### 実行コマンド
```bash
mvn module-analyzer:list-expose -DrootDir=modules
```

### パラメータ
- `rootDir`: モジュールのルートディレクトリパス（必須）
- `dependencyTo`: 各モジュールが依存している他のモジュールを表示（デフォルト: false）
- `dependencyFrom`: 各モジュールが依存されている他のモジュールを表示（デフォルト: false）

### プロジェクト構成例
```
modules/
├── order/
│   ├── src/main/java/com/example/order/expose/
│   │   ├── OrderApi.java
│   │   └── OrderDto.java
│   └── service/
│       └── OrderService.java  # ProductApiとUserApiを使用
├── user/
│   └── src/main/java/com/example/user/expose/
│       ├── UserApi.java
│       └── UserDto.java
└── product/
    └── src/main/java/com/example/product/expose/
        └── ProductApi.java
```

### 実行結果

#### 基本実行（依存関係なし）
```bash
$ mvn module-analyzer:list-expose -DrootDir=modules

[INFO] [Module: order]
[INFO]   - com.example.order.expose.OrderApi
[INFO]   - com.example.order.expose.OrderDto
[INFO]
[INFO] [Module: product]
[INFO]   - com.example.product.expose.ProductApi
[INFO]
[INFO] [Module: user]
[INFO]   - com.example.user.expose.UserApi
[INFO]   - com.example.user.expose.UserDto
```

#### dependencyTo オプション（依存先を表示）
```bash
$ mvn module-analyzer:list-expose -DrootDir=modules -DdependencyTo=true

[INFO] [Module: order]
[INFO]   - com.example.order.expose.OrderApi
[INFO]   - com.example.order.expose.OrderDto
[INFO]   Dependencies to:
[INFO]     - product
[INFO]     - user
[INFO]
[INFO] [Module: product]
[INFO]   - com.example.product.expose.ProductApi
[INFO]
[INFO] [Module: user]
[INFO]   - com.example.user.expose.UserApi
[INFO]   - com.example.user.expose.UserDto
```

#### dependencyFrom オプション（依存元を表示）
```bash
$ mvn module-analyzer:list-expose -DrootDir=modules -DdependencyFrom=true

[INFO] [Module: order]
[INFO]   - com.example.order.expose.OrderApi
[INFO]   - com.example.order.expose.OrderDto
[INFO]
[INFO] [Module: product]
[INFO]   - com.example.product.expose.ProductApi
[INFO]   Depended by:
[INFO]     - order
[INFO]
[INFO] [Module: user]
[INFO]   - com.example.user.expose.UserApi
[INFO]   - com.example.user.expose.UserDto
[INFO]   Depended by:
[INFO]     - order
```

#### 両方のオプションを同時に使用
```bash
$ mvn module-analyzer:list-expose -DrootDir=modules -DdependencyTo=true -DdependencyFrom=true

[INFO] [Module: order]
[INFO]   - com.example.order.expose.OrderApi
[INFO]   - com.example.order.expose.OrderDto
[INFO]   Dependencies to:
[INFO]     - product
[INFO]     - user
[INFO]
[INFO] [Module: product]
[INFO]   - com.example.product.expose.ProductApi
[INFO]   Depended by:
[INFO]     - order
[INFO]
[INFO] [Module: user]
[INFO]   - com.example.user.expose.UserApi
[INFO]   - com.example.user.expose.UserDto
[INFO]   Depended by:
[INFO]     - order
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
