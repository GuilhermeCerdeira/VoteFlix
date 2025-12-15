JCC = javac
JVM = java

SRC_DIR = src
BIN_DIR = bin
LIB_DIR = lib
PATH_TO_FX = javafx-lib

SEP = ;
RM = rmdir /s /q
MKDIR = -mkdir 
COPY = copy /Y

CSS_SRC = src\br\com\guilhermando\cliente\style.css
CSS_DEST = bin\br\com\guilhermando\cliente\style.css

CP_ALL = "bin;lib/*;javafx-lib/*"

SOURCES = $(subst \,/,$(shell dir $(SRC_DIR)\*.java /s /b))


all: compile copy-resources

compile:
	@echo "Verificando diretorio $(BIN_DIR)..."
	$(MKDIR) $(BIN_DIR)
	@echo "Compilando fontes..."
	$(JCC) -d $(BIN_DIR) -cp $(CP_ALL) $(SOURCES)

copy-resources:
	@echo "Copiando CSS..."
	$(COPY) "$(CSS_SRC)" "$(CSS_DEST)"

server: compile
	@echo "Verificando diretorio data..."
	$(call MKDIR_P,data)
	@echo "Iniciando Interface do Servidor..."
	$(JVM) "-Djava.library.path=$(PATH_TO_FX)" "-Dprism.order=sw" --module-path $(PATH_TO_FX) --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics -cp $(CP_ALL) br.com.guilhermando.servidor.ServerLauncher

client: compile
	@echo "Iniciando cliente (Terminal)..."
	$(JVM) -cp $(CP_ALL) br.com.guilhermando.cliente.client

gui: compile copy-resources
	@echo "Iniciando Interface Grafica..."
	$(JVM) "-Djava.library.path=$(PATH_TO_FX)" "-Dprism.order=sw" --module-path $(PATH_TO_FX) --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics -cp $(CP_ALL) br.com.guilhermando.cliente.Launcher

.PHONY: clean
clean:
	@echo "Limpando tudo..."
	$(RM) $(BIN_DIR)
	$(RM) data