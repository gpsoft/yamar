USERID ?= 1764261

CMD_LIST := dev run uber

all:
	@echo Usage:
	@echo make dev
	@echo make run
	@echo make run-details
	@echo make uber

.PHONY: $(CMD_LIST)
.SILENT: $(CMD_LIST)

dev:
	clj -M:dev

run:
	clojure -M -m yamar.core $(USERID)

run-details:
	clojure -M -m yamar.core -D $(USERID)

uber:
	clojure -T:build clean
	clojure -T:build uber

%:
	@:

