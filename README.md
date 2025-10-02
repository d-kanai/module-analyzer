# 🔍 Module Analyzer Maven Plugin

モジュラモノリス構成のプロジェクトにおいて、各モジュールの公開API（expose）を可視化するMavenプラグインです。

## ✨ 機能

### 📋 公開API一覧表示

各モジュールの `expose` ディレクトリ配下にあるJavaクラスを一覧表示します。

## 🚀 使い方

### インストール

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

### コマンド実行

```bash
mvn module-analyzer:list-expose -DrootDir=modules
```

**パラメータ:**
- `rootDir`: モジュールのルートディレクトリパス（必須）

## 📊 実行例

### プロジェクト構成

```
modules/
├── order/
│   └── src/main/java/com/example/order/expose/
│       ├── OrderApi.java
│       └── OrderDto.java
├── user/
│   └── src/main/java/com/example/user/expose/
│       ├── UserApi.java
│       └── UserDto.java
└── product/
    └── src/main/java/com/example/product/expose/
        └── ProductApi.java
```

### 実行結果

```bash
$ mvn module-analyzer:list-expose -DrootDir=modules

[INFO] --- module-analyzer:v1.0.3-SNAPSHOT:list-expose (default-cli) @ test-project ---
[INFO]
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
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

