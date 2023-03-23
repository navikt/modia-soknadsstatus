rootProject.name = "modia-soknadsstatus"
include("common:jms")
include("common:kafka")
include("common:kafka-stream-transformer")
include("common:dataformat")
include("common:ktor")
include("apps:mq-to-kafka")
include("apps:arena-infotrygd-soknadsstatus-transform")
include("apps:foreldrepenger-soknadsstatus-transform")
include("apps:pleiepenger-soknadsstatus-transform")
include("apps:modia-soknadsstatus-api")
include("apps:data-generator-app")
include("apps:fp-k9-soknadsstatus-transform")
include("common:filter")
