rootProject.name = "unplugged-afk"

include("common")
include("paper")
include("paper-26.1.2")
include("paper-26.2")
include("velocity")

project(":paper-26.1.2").projectDir = file("paper/versions/26.1.2")
project(":paper-26.2").projectDir = file("paper/versions/26.2")
