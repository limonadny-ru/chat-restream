lint:
	clj-kondo --lint src

NAME = chat-restream

NI_TAG = ghcr.io/graalvm/native-image:22.2.0

NI_ARGS = \
	--initialize-at-build-time \
	--report-unsupported-elements-at-runtime \
	--no-fallback \
	-jar ${JAR} \
	-J-Dfile.encoding=UTF-8 \
	--enable-http \
	--enable-https \
	-H:+PrintClassInitialization \
	-H:+ReportExceptionStackTraces \
	-H:Log=registerResource \
	-H:Name=./builds/${NAME}-

PLATFORM = PLATFORM

JAR = target/uberjar/${NAME}.jar

DATE = $(shell date +%s)


platform-docker:
	docker run -it --rm --entrypoint /bin/sh ${NI_TAG} -c 'echo `uname -s`-`uname -m`' > ${PLATFORM}

build-binary-docker: uberjar platform-docker
	docker run -it --rm -v ${PWD}:/build -w /build ${NI_TAG} ${NI_ARGS}$(shell cat ${PLATFORM})

platform-local:
	echo `uname -s`-`uname -m` > ${PLATFORM}

graal-build: platform-local
	native-image ${NI_ARGS}$(shell cat ${PLATFORM})

build-binary-local: uberjar graal-build


uberjar:
	lein uberjar

zip:
	zip -j target/${NAME}.zip conf/handler.sh builds/${NAME}-Linux-x86_64

bash-package: build-binary-docker zip

run-test:
	lein compile && lein install && lein run $(token)

upload-version:
	aws --endpoint-url=https://storage.yandexcloud.net/ \
		s3 cp target/${NAME}.zip s3://lmnd/${NAME}.zip

deploy-version:
	yc serverless function version create \
		--function-name=${NAME} \
		--runtime bash \
		--entrypoint handler.sh \
		--memory 128m \
		--execution-timeout 3s \
		--environment TELEGRAM_BOT_TOKEN=$(token) \
		--package-bucket-name lmnd \
		--package-object-name ${NAME}.zip

deploy: upload-version deploy-version

set-webhook:
	curl 'https://api.telegram.org/bot$(token)/setWebhook?url=https://functions.yandexcloud.net/$(id)'

all: bash-package deploy
