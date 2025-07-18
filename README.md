# 🌱 VainillApp

Aplicación móvil desarrollada para apoyar a los agricultores de Bahía Solano (Chocó, Colombia)
en el **monitoreo del estado de los cultivos de vainilla**, mediante la captura y análisis de 
imágenes, alertas personalizadas y almacenamiento de información fenológica.

## 📲 Instalación

### 🔧 Desde Android Studio

1. Clona este repositorio:
   ```bash
   git clone https://github.com/aleospiria/VainillApp.git
2. Abre la carpeta source/ en Android Studio.

3. Conecta un dispositivo físico o emulador.

4. Compila y ejecuta la app.

### 📦 Instalación directa (APK)
Puedes instalar la APK manualmente en tu celular aqui:
[VainillApp.apk](apk/VainillApp.apk)

Asegúrate de habilitar la instalación desde orígenes desconocidos en tu dispositivo.

## Fecha: 15 de Enero de 2025, 15/01/2025

## 🎯 Objetivo del Proyecto

Desarrollar una aplicación móvil que permita a los agricultores de vainilla en Bahía Solano, 
Chocó, realizar el monitoreo del estado de los cultivos, recibir alertas para polinización y 
cosecha, para la mejora de la gestión del cultivo. 

### ✅ Requisitos funcionales

  1. Registro de datos fenológicos off-line: 
  Campos: tipo de planta, fechas de floración, polinización, crecimiento de 
  frutos y maduración. 
  2. Notificaciones y alertas: 
  Enviar recordatorios sobre actividades como fertilización, polinización y 
  cosecha. Además, estas notificaciones deben ser tanto en texto para 
  visualizar en la pantalla del dispositivo móvil como auditivas para personas 
  que no puedan leer. 
  3. Base de datos off-line: 
  Almacenar información ingresada por el usuario para consultar 
  posteriormente. 
  4. Interfaz sencilla y amigable: 
  Facilitar la entrada de datos y la visualización del progreso del cultivo. 
  5. Herramienta cognitiva: 
  Implementar una red neuronal convolucional para reconocer el estado del 
  cultivo (sujeto a imágenes clasificadas que son provistas por el cliente). 
  6. Se espera que entre el tipo de usuarios haya personas que pueden tener 
  limitaciones o nulo conocimiento en lectura.

### ⚙️ Requisitos no funcionales

  1. Plataforma de desarrollo: Uso de un framework nativo con dispositivos 
  Android. 
  2. Diseño: Interfaz intuitiva con principios de diseño UX/UI. 
  3. Desempeño y optimización: Aplicación ligera, capaz de ejecutarse off-line en 
  dispositivos móviles de gama media.

## 🏗️ Arquitectura del Proyecto

La arquitectura de la aplicación móvil para el seguimiento fenológico de la vainilla se ha 
diseñado de manera que optimice tanto el uso en línea (on-line) como fuera de línea 
(off-line), mediante la interconexión de tres bloques principales, tal como se muestra en la 
Figura 1: Interfaz de Usuario, Backend Local, y Backend Nube. A continuación se 
describe en detalle cada uno de estos bloques y la relación entre sus componentes: 

### 1. 🧑‍💻 Interfaz de Usuario (UI)
La Interfaz de Usuario es el punto de interacción entre el usuario y la aplicación. Esta está 
formada por tres componentes clave, cada uno diseñado para facilitar distintas tareas 
relacionadas con el seguimiento y análisis de los cultivos:
- **Captura de imagen:** Este componente permite que el usuario tome fotografías 
utilizando la cámara del dispositivo móvil. Estas imágenes se capturan para su 
posterior análisis, lo que es esencial para el seguimiento visual de la fenología de la 
vainilla. Las imágenes adquiridas son procesadas posteriormente por el Backend 
Local utilizando redes neuronales convolucionales (CNN)  entrenadas. 
- **Notificaciones:**  La aplicación tiene la capacidad de enviar notificaciones al usuario, 
lo que le permite recibir alertas sobre eventos importantes, como el estado de los 
cultivos o cualquier otra acción relevante que haya sido determinada por las 
condiciones definidas en el Backend Local. Las notificaciones son procesadas y 
gestionadas por el Backend Local, pero se visualizan a través de la Interfaz de 
Usuario para una experiencia más interactiva. 
- **Entrada de datos:** El usuario puede ingresar manualmente información en la 
aplicación a través de la pantalla táctil del dispositivo. Esto incluye datos 
relacionados con el cultivo, observaciones, o cualquier información que no sea 
capturada por la cámara. Estos datos se almacenan localmente en el dispositivo y 
pueden ser sincronizados con el Backend Nube cuando haya conectividad a 
internet.

### 2. 📦 Backend Local
El Backend Local es un bloque crítico que garantiza que la aplicación funcione 
correctamente incluso sin conexión a internet (off-line), gestionando operaciones esenciales 
como el procesamiento de imágenes, el almacenamiento de datos y la generación de 
notificaciones. Está compuesto por varios componentes clave: 
- **Red Convolucional:** Este componente es responsable del procesamiento y análisis 
de las imágenes capturadas por el usuario. Se utiliza una red neuronal convolucional 
(CNN) entrenada para clasificar y analizar las imágenes en función de las 
condiciones fenológicas del cultivo de vainilla. Esto incluye la identificación de los 
diferentes estadios de crecimiento de la planta, especialmente en la floración. 
- **Base de Datos Local:** La base de datos local almacena toda la información 
generada en el dispositivo móvil, incluyendo imágenes, datos ingresados por el 
usuario y otros eventos generados por la aplicación. Cuando el dispositivo recupera 
la conectividad a internet, esta base de datos local puede sincronizarse con el 
Backend Nube para consolidar la información si así se desea.
- **Sistema de Notificaciones:** El sistema de notificaciones envía alertas al usuario en 
función de los datos obtenidos del procesamiento de las imágenes o de los datos 
ingresados manualmente. El sistema asegura que el usuario esté informado sobre 
eventos importantes relacionados con los cultivos. 
- **Verificación de conexión:** Este componente monitorea el estado de la conexión a 
internet del dispositivo. Si el dispositivo está sin conexión, la aplicación sigue 
operando localmente, pero si la conexión es restaurada, la Verificación de 
Conexión coordina la sincronización de los datos entre la base de datos local y el 
Backend Nube, si el usuario tiene la opción activada y configurada.

### 3. ☁️ Backend en la Nube
El Backend Nube complementa la funcionalidad de la aplicación en modo en línea (on-line), 
proporcionando almacenamiento centralizado y sincronización de los datos entre diferentes 
dispositivos. Este bloque también optimiza la mejora continua del modelo de la red neuronal 
para el análisis de las imágenes de los cultivos. Sus componentes son los siguientes: 
- **Base de Datos Global:** La base de datos global almacena la 
información consolidada y sincronizada desde múltiples dispositivos del mismo 
usuario. Además, puede almacenarse en la nube a través de plataformas como 
Google Drive, lo que le da flexibilidad y escalabilidad para adaptarse a las 
necesidades de los usuarios.   
- **Actualización del modelo CNN:** Este componente es crucial para el 
aprendizaje y la mejora continua del sistema. El modelo de red neuronal utilizado 
para analizar las imágenes puede ser actualizado mediante el envío de nuevos 
datos desde los dispositivos móviles. Las imágenes que han sido capturadas y 
clasificadas por los usuarios se utilizan para entrenar de nuevo el modelo de red 
neuronal, que posteriormente es descargado al Backend Local para mejorar su 
capacidad de reconocimiento.

### 🔄 Flujo de Datos y Sincronización 
- **Offline:** Cuando no hay conectividad a internet, la aplicación funciona de manera 
autónoma, con la Captura de Imagen y la Entrada de Datos procesándose 
localmente en el dispositivo móvil. Las imágenes se analizan mediante la Red 
Convolucional Local, y los datos se almacenan en la Base de Datos Local. Las 
notificaciones se generan a través del sistema de notificaciones local, y el estado de 
la conexión se verifica constantemente. 
- **Online:** Cuando el dispositivo se conecta a internet, la Verificación de Conexión 
sincroniza la Base de Datos Local con la Base de Datos Global en el Backend 
Nube, asegurando que todos los datos estén centralizados. Además, el modelo de 
red neuronal se actualiza con nuevos datos de clasificación, mejorando el sistema 
de análisis en el futuro.

En resumen, la arquitectura propuesta para la aplicación móvil permite un seguimiento 
fenológico de la vainilla y cultivos, tanto en condiciones de conectividad (on-line) como fuera 
de línea (off-line) , optimizando el procesamiento de imágenes, la entrada de datos, la 
gestión de notificaciones y la sincronización en la nube. Esta estructura modular y flexible 
asegura una experiencia de usuario fluida y un sistema que mejora continuamente su 
capacidad de análisis y predicción. 

<img width="812" height="687" alt="image" src="https://github.com/user-attachments/assets/34bb09e3-1c79-4024-a2ac-0ac98701ebc4" />

Figura 1. Arquitectura de la aplicación móvil. 

## 📊 Informe técnico de desempeño de la app (Primera versión, 1.0)
### Fecha: 4 de Junio de 2025, 04/06/2025
###  Evaluación del modelo de visión artificial (primera version, 1.0)
## 1. Introducción
Este informe presenta los resultados de la primera versión del componente de visión 
artificial (inteligencia artificial que "ve" e interpreta imágenes) desarrollado para la 
aplicación de identificación del estado de los cultivos de vainilla, para los agricultores de 
Bahía Solano, Chocó. El objetivo de esta tecnología es ayudar a los agricultores a 
identificar las diferentes etapas de desarrollo de la planta de vainilla utilizando la cámara 
de sus dispositivos móviles basados en Android (por ejemplo, si están en floración o listas 
para cosechar), y enviar alertas sobre tareas importantes, como la polinización. Este 
informe explica los resultados de las pruebas de esta componente de visión artificial, qué 
tan bien funciona y qué significa para los usuarios. También se presentan 
recomendaciones para mejorar la aplicación en el futuro. 
## 2. Descripción de la Aplicación 
La aplicación permite: 
  1. Tomar fotos de plantas de vainilla con el teléfono. 
  2. Identificar el estado de la planta (floración, fruto maduro, etc.). 
  3. Registrar datos manuales, como fechas de floración. 
  4. Recibir alertas para polinizar o cosechar.
     
La componente de visión artificial usa una red neuronal convolucional, una inteligencia 
artificial que analiza fotos para reconocer patrones. El modelo de inteligencia artificial fue 
entrenado utilizando la plataforma TensorFlow y luego adaptado para su uso en 
dispositivos móviles (TFLite). Fue entrenada para detectar cinco estados: Floración, Fruto, 
Vegetacion, Posible Polinizada y No Polinizada. Para evaluar su desempeño, se utilizaron 
métricas estándar en la industria de la visión artificial.

## 3. Resultados de las Pruebas y su significado 
<img width="1181" height="743" alt="image" src="https://github.com/user-attachments/assets/d6960f38-055d-4542-aa6d-0069aebf4f33" />

**Figura 2. Métricas de desempeño.**

Estas métricas se utilizan para entender qué tan bien está funcionando el modelo de 
inteligencia artificial al clasificar las imágenes de las plantas de vainilla en sus diferentes 
etapas de desarrollo. 

### Precisión (Precision) 
  - **Qué mide:** De todas las veces que el modelo predijo una etapa específica (por 
ejemplo, "Fruto"), ¿qué porcentaje de esas predicciones fue realmente correcto?
  - **En el contexto de la vainilla:** Si el modelo identifica 10 imágenes como "Fruto", y 
7 de ellas son realmente frutos, la precisión para la clase "Fruto" sería del 70%. 
Una alta precisión significa que cuando el modelo dice que es una etapa particular, 
es muy probable que esté en lo cierto. Evita los "falsos positivos" (decir que es 
algo que no es). 
  - **Fórmula conceptual:** Verdaderas predicciones positivas / (Verdaderas 
predicciones positivas + Falsas predicciones positivas). 
  - **Valor en el informe:** El informe indica una precisión general de 0.2280 y 
ponderado de 0.3622.
### Recall (Sensibilidad o Exhaustividad) 
  - **Qué mide:** De todas las imágenes que realmente pertenecían a una etapa 
específica (por ejemplo, todos los verdaderos "Frutos" en el conjunto de prueba), 
¿qué porcentaje logró identificar correctamente el modelo? 
  - **En el contexto de la vainilla:** Si había 10 imágenes de "Fruto" en los datos de 
prueba, y el modelo encontró 8 de ellas, el recall para la clase "Fruto" sería del 
80%. Un alto recall significa que el modelo es bueno encontrando la mayoría de 
las instancias de una etapa particular. Evita los "falsos negativos" (no identificar 
algo que sí es). 
  - **Fórmula conceptual:** Verdaderas predicciones positivas / (Verdaderas 
predicciones positivas + Falsas predicciones negativas).
  - **Valor en el informe:** El informe indica un recall general de 0.2200 y ponderado de 
0.3478. 
### F1-Score (Puntuación F1) 
  - **Qué mide:** Es una media armónica entre Precisión y Recall. Ofrece una medida 
balanceada del rendimiento del modelo, especialmente útil cuando hay un 
desequilibrio en las clases (algunas etapas tienen muchas más imágenes que 
otras) o cuando tanto la precisión como el recall son importantes. Un F1-Score alto 
indica que el modelo tiene tanto buena precisión como buen recall.
  - **En el contexto de la vainilla:** Si el modelo es muy preciso para "Fruto" pero omite 
muchos (bajo recall), o si identifica casi todos los frutos (alto recall) pero también 
clasifica erróneamente muchas otras cosas como fruto (baja precisión), el 
F1-Score será más bajo. Busca un equilibrio. Es especialmente relevante en este 
caso porque el informe menciona "datos tan limitados" y "clases con pocos 
ejemplos", lo que sugiere un desequilibrio. 
  - **Fórmula conceptual:** 2 * (Precisión * Recall) / (Precisión + Recall). 
  - **Valor en el informe:** El informe indica un F1-Score general de 0.2232 y ponderado 
de 0.3541. 
### Support (Soporte) 
  - **Qué mide:** Simplemente indica el número real de muestras (imágenes) de cada 
clase o etapa que se utilizaron en el conjunto de datos de prueba para calcular las 
métricas anteriores. 
  - **En el contexto de la vainilla:** En la tabla de metricas, la columna "support" 
muestra cuántas imágenes había de cada etapa fenológica. Por ejemplo, si para 
"Inflorescencia" el soporte es "2", significa que solo había 2 imágenes de 
inflorescencias en el conjunto de prueba.
  - **Importancia:** Un soporte bajo para una clase específica (como Inflorescencia y 
Posible Polinizada) significa que las métricas de precisión, recall y F1-score para 
esa clase son menos confiables, ya que se basan en muy pocos ejemplos. Esto 
explica por qué estas clases tuvieron un desempeño nulo: el modelo tuvo muy 
pocos ejemplos para aprender y ser evaluado.

En resumen, estas métricas ayudan a entender los puntos fuertes y débiles del modelo de 
visión artificial. La precisión le dice cuán confiables son las predicciones positivas del 
modelo, el recall cuán completo es el modelo para encontrar todas las instancias de una 
clase, el F1-Score ofrece un balance entre ambas, y el support le da contexto sobre 
cuántos datos respaldan esas métricas para cada etapa de la vainilla  


<img width="952" height="817" alt="image" src="https://github.com/user-attachments/assets/fc8a495b-7bdb-4260-a670-d8ae947ca0b6" />

**Figura 3. Matriz de confusión.**

En el contexto de la evaluación de visión artificial para el monitoreo de cultivos de vainilla, 
la matriz de confusión es una herramienta visual muy importante que permite entender 
en detalle cómo se está comportando el modelo de inteligencia artificial al clasificar las 
imágenes de las diferentes etapas de la planta. 

Básicamente, es una tabla que compara las predicciones que hizo el modelo con las 
etapas reales a las que pertenecían las imágenes. Le muestra no solo cuándo el modelo 
acierta, sino, lo que es más crucial, cuándo se equivoca y cómo se equivoca (es decir, qué 
etapa confunde con qué otra etapa). 

### Cómo se organiza y qué significa: 

  - Cada fila de la matriz representa las instancias reales de una clase (por ejemplo, 
todas las imágenes que realmente son "Floración").
  - Cada columna representa las predicciones hechas por el modelo para cada clase 
(por ejemplo, todas las imágenes que el modelo dijo que eran "Floración").

Dentro de la tabla, los números le indican: 

  - **Aciertos (Diagonal Principal):** Los números en la diagonal de la matriz (de la 
esquina superior izquierda a la inferior derecha) le muestran cuántas veces el 
modelo clasificó correctamente una etapa. Por ejemplo, cuántas imágenes que 
eran realmente "Fruto" el modelo las identificó como "Fruto" 1. El informe indica 
que "Fruto es la clase mejor reconocida por el modelo", lo que se vería reflejado en 
un número relativamente alto en la celda donde la fila "Fruto" se cruza con la 
columna "Fruto".
  - **Errores (Fuera de la Diagonal):** Los números fuera de esta diagonal principal 
representan los errores o "confusiones" del modelo.
    - Por ejemplo, el informe menciona: "Floración es frecuentemente confundida 
con Fruto y Posible Polinizada". Esto significa que en la fila 
correspondiente a la etapa real "Floración", aparecerían números en las 
columnas de "Fruto" y "Posible Polinizada", indicando que el modelo 
incorrectamente etiquetó imágenes de floración como si fueran fruto o 
posible polinizada.
    - De manera similar, Inflorescencia y Posible Polinizada presentan 
confusiones significativas. Esto indica que el modelo tiene dificultades para 
distinguir estas etapas entre sí o con otras.

### ¿Qué información clave proporciona la matriz de confusión en este contexto?

  - **Identificación de Confusiones Específicas:** Permite ver exactamente qué 
etapas el modelo tiende a confundir. Saber que Floración es frecuentemente 
confundida con Fruto es mucho más útil que solo saber que la precisión general es 
baja.
  - **Diagnóstico de Problemas:** Ayuda a entender por qué el modelo podría estar 
fallando.Vincula las confusiones de Inflorescencia y Posible Polinizada con el 
hecho de que el modelo no ha aprendido sus características distintivas debido a 
los datos limitados que han sido dado al modelo para su entrenamiento. Esto 
sugiere que se necesitan más imágenes de estas etapas.
  - **Guía para Mejoras:** Al destacar los errores específicos, la matriz de confusión 
guía las acciones para mejorar el modelo. Si el modelo confunde "Floración" con 
"Fruto", se pueden buscar más imágenes que ayuden al modelo a aprender las 
diferencias sutiles entre estas dos etapas.
  - **Evaluación Detallada del Rendimiento:** Ofrece una visión más completa que 
métricas únicas como la exactitud general, especialmente cuando algunas clases 
tienen muy pocos ejemplos (como "Inflorescencia" y "Posible Polinizada").

En resumen, la matriz de confusión es una radiografía del desempeño del modelo, 
mostrando no solo si acierta o falla, sino precisamente dónde ocurren los errores de 
clasificación entre las diferentes etapas fenológicas de la vainilla. Esto es fundamental 
para entender las limitaciones actuales de la aplicación y para planificar cómo mejorarla, 
especialmente en la correcta identificación de etapas cruciales para la polinización. 

## 4. Desempeño General 

La visión artificial identifica correctamente el estado de las plantas en cerca del 34.78% de 
los casos (F1-Score de 0.5128 para la mejor clase). Es útil para algunas tareas, pero 
necesita mejoras. 

## 5. Desempeño por Estado de la Planta 

  - **Fruto:** Mejor reconocido, con 51.28% de precisión.
  - **Floración:** A veces se confunde con Fruto o Posible Polinizada, lo que puede 
causar errores en alertas de polinización.
  - **Inflorescencia y Posible Polinizada:** No se identifican correctamente por falta de 
fotos de entrenamiento.

## 6. Problemas Identificados 

  - **Pocos datos:** La red neuronal necesita más fotos, especialmente de 
Inflorescencia y Posible Polinizada.
  - **Confusiones:** Floración y Fruto se parecen en fotos, confundiendo a la aplicación.
  - **Limitaciones técnicas:** La aplicación es simple para Android, pero la visión 
artificial requiere más datos y ajustes.

## 7. Implicaciones para los Agricultores 

Esta versión inicial tiene limitaciones

  - **Útil para frutos:** Detecta frutos maduros con buena precisión, ayudando en la 
cosecha.
  - **No confiable para polinización:** Los errores en Floración, Inflorescencia y Posible 
Polinizada hacen que no sea suficiente para decidir cuándo polinizar. Combine la 
aplicación con su experiencia.
  - **Registro manual:** Sigue siendo útil para llevar control de cultivos.

## 8. Puntos a Tener en Cuenta para Mejorar la Aplicación 

  - **Recolectar más fotos:** Proporcionar más imágenes de todos los estados, 
especialmente Inflorescencia y Posible Polinizada.
  - **Mejorar el modelo:** Usar técnicas para que la aplicación aprenda mejor los 
estados con pocos ejemplos.
  - **Funciones avanzadas:** En el futuro, integrar sensores o imágenes satelitales (no 
incluido ahora).
  - **Capacitación:** Ofrecer talleres para que los agricultores usen la aplicación 
correctamente.

## 9. Análisis General y Próximos Pasos 

El desempeño actual del modelo de visión artificial es un punto de partida. Las clases con 
pocos ejemplos de entrenamiento (Inflorescencia y Posible Polinizada) tuvieron un 
desempeño nulo, lo que subraya la necesidad crítica de obtener más imágenes de estas 
etapas para mejorar la capacidad de identificación del modelo. 

Para mejorar la fiabilidad de la aplicación, especialmente en las alertas de polinización y 
en la distinción precisa de todas las etapas fenológicas, se recomienda: 
    - **Incrementar el Volumen de Datos de Entrenamiento:** Recopilar una cantidad 
significativamente mayor de imágenes para todas las clases, con un enfoque 
especial en aquellas donde el modelo tuvo bajo desempeño (Inflorescencia, 
Posible Polinizada). Es crucial que estas imágenes sean variadas y 
representativas de las condiciones reales del cultivo. 
    - **Balanceo de Clases:** Aplicar técnicas para asegurar que el modelo reciba una 
cantidad más equilibrada de ejemplos de cada etapa, o utilizar métodos que 
compensen el desequilibrio durante el entrenamiento. 
    - **Reentrenamiento y Ajuste del Modelo:** Una vez se disponga de más datos, el 
modelo deberá ser re entrenado y ajustado para mejorar su precisión general y 
específica por clase. 

Estos pasos son fundamentales para que la herramienta cognitiva de visión artificial 
pueda cumplir eficazmente con el objetivo de ayudar a los agricultores de Bahía Solano a 
monitorear sus cultivos de vainilla y tomar decisiones informadas sobre polinización y 
cosecha, tal como se describe en la propuesta de desarrollo de la aplicación. 

## Conclusión del primer informe de seguimiento

La primera versión de la aplicación es un avance para los agricultores de Bahía Solano. 
La visión artificial detecta frutos maduros con aceptable precisión, pero tiene problemas 
con otros estados por falta de fotos. Es útil para registrar datos y enviar alertas básicas. 
Con más imágenes y ajustes, la próxima versión será más precisa. Recomendamos usar 
la aplicación junto con la experiencia de los agricultores para mejores resultados. 

