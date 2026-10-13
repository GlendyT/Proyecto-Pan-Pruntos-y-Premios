# Diagramas UML y flujos

Los diagramas se mantienen como Mermaid para poder editarlos junto con el código. GitHub y varios editores Markdown pueden renderizarlos; si el visor no lo hace, conserva los bloques de código o utiliza un renderizador Mermaid.

## 1. Despliegue

```mermaid
flowchart LR
    Browser[Navegador del cliente]
    subgraph Vercel[Proyecto Next.js en Vercel]
      Next[Next.js App Router]
      Rewrite[Rewrite /api/*]
      Next --> Rewrite
    end
    subgraph OCI[VM OCI pan-puntos-api]
      Nginx[Nginx + certificado TLS]
      API[Spring Boot :8080, local]
      Wallet[Wallet Oracle privado]
      Nginx --> API
      API --> Wallet
    end
    ADB[(Autonomous Database existente)]
    EJS[EmailJS]
    Mail[Correo del cliente]
    Browser -->|HTTPS| Next
    Rewrite -->|HTTPS| Nginx
    API -->|JDBC/mTLS| ADB
    API -->|HTTPS REST| EJS
    EJS --> Mail
    Swing[Aplicación Swing autorizada] -->|JDBC/wallet| ADB
```

## 2. Secuencia de autenticación por enlace

```mermaid
sequenceDiagram
    actor Cliente
    participant Web as Portal Next.js
    participant API as Spring Boot
    participant DB as Oracle ADB
    participant Mail as EmailJS/correo
    Cliente->>Web: Escribe correo
    Web->>API: POST /api/v1/auth/access-requests
    API->>DB: Buscar cliente y aplicar cooldown/límites
    API->>DB: Guardar hash del token y vencimiento
    API->>Mail: Solicitar entrega del enlace
    Mail-->>Cliente: URL /auth/callback#token=...
    Cliente->>Web: Abre URL
    Web->>Web: Leer token del fragmento y limpiar URL
    Web->>API: POST /api/v1/auth/consume
    API->>DB: Validar y consumir token de un solo uso
    API-->>Web: Crear sesión + cookie segura
    Web->>API: GET /api/v1/me
    API->>DB: Cargar perfil del principal autenticado
    API-->>Web: Datos personales con no-store
```

## 3. Actividad: solicitud e inicio de sesión

```mermaid
flowchart TD
    A[Cliente introduce correo] --> B[POST access-requests]
    B --> C{Formato y límite válidos?}
    C -- No --> D[Mensaje genérico/validación]
    C -- Sí --> E{Cuenta activa y se permite envío?}
    E -- No --> F[Respuesta genérica; no revelar existencia]
    E -- Sí --> G[Guardar hash y expiración del token]
    G --> H[EmailJS envía enlace]
    H --> I{Cliente abre enlace antes de vencer?}
    I -- No --> J[Solicitar enlace nuevo]
    I -- Sí --> K[Frontend envía token por POST]
    K --> L{Token correcto, vigente y sin uso?}
    L -- No --> J
    L -- Sí --> M[Consumir token y crear sesión]
    M --> N[Consultar perfil y puntos propios]
```

## 4. Secuencia de pedido y cobro en Swing

```mermaid
sequenceDiagram
    actor Personal
    participant UI as Formularios Swing
    participant S as Pedido/Venta Servicio
    participant DAO as DAO JDBC
    participant DB as Oracle ADB
    Personal->>UI: Seleccionar cliente e ítems
    UI->>S: Validar y crear pedido
    S->>DAO: Guardar pedido pendiente y detalle
    DAO->>DB: INSERT transaccional
    Personal->>UI: Cobrar pedido
    UI->>S: Cobrar efectivo/tarjeta simulada
    S->>DAO: Revalidar estado, stock y pago
    DAO->>DB: Bloquear filas y descontar existencia
    DAO->>DB: Registrar venta, movimientos, puntos y estado
    alt Todas las operaciones correctas
      DB-->>S: Commit
      S-->>UI: Comprobante
    else Rechazo o error
      DB-->>S: Rollback
      S-->>UI: Error de negocio sin cambios parciales
    end
```

## 5. Actividad: consulta de puntos

```mermaid
flowchart TD
    A[Cliente abre portal] --> B{Cookie de sesión válida?}
    B -- No --> C[401 y solicitar enlace]
    B -- Sí --> D[API obtiene ClientePrincipal]
    D --> E[DAO consulta puntos/movimientos para ese cliente]
    E --> F{Consulta exitosa?}
    F -- No --> G[Mostrar error de carga; conservar privacidad]
    F -- Sí --> H[Responder JSON no-store]
    H --> I[Mostrar saldo e historial]
```

## 6. Clases por capas

```mermaid
classDiagram
    class FormReportes
    class ReporteServicio
    class ReporteDAO
    class ReporteDAOImpl
    class FormPedido
    class PedidoServicio
    class VentaServicio
    class PedidoDAO
    class PedidoDAOImpl
    class PuntosYCanjesServicio
    class CanjeDAO
    class CanjeDAOImpl
    class AuthController
    class AuthService
    class AuthDAO
    class AuthDAOImpl
    class PuntosController
    class PuntosConsultaService
    class PuntosConsultaDAO
    class PuntosConsultaDAOImpl
    FormReportes --> ReporteServicio
    ReporteServicio --> ReporteDAO
    ReporteDAO <|.. ReporteDAOImpl
    FormPedido --> PedidoServicio
    PedidoServicio --> PedidoDAO
    PedidoDAO <|.. PedidoDAOImpl
    VentaServicio --> PedidoDAO
    PuntosYCanjesServicio --> CanjeDAO
    CanjeDAO <|.. CanjeDAOImpl
    AuthController --> AuthService
    AuthService --> AuthDAO
    AuthDAO <|.. AuthDAOImpl
    PuntosController --> PuntosConsultaService
    PuntosConsultaService --> PuntosConsultaDAO
    PuntosConsultaDAO <|.. PuntosConsultaDAOImpl
```
