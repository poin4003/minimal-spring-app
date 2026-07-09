-include .env

ifeq ($(OS),Windows_NT)
    SHELL = cmd.exe
    MVN_CMD = mvnw.cmd
else
    MVN_CMD = ./mvnw
endif

dev:
	@echo "Starting server in DEV mode..."
	$(MVN_CMD) spring-boot:run -Dspring-boot.run.profiles=dev

prod:
	@echo "Starting server in PROD mode..."
	$(MVN_CMD) spring-boot:run -Dspring-boot.run.profiles=prod
