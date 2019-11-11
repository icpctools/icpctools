java_library(
    name = "presentations",
    srcs = glob(["**/*.java"]),
    deps = [
        "//ContestModel/src:contestModel",
        "//PresCore/src:presentCore",
        "//PresContest/lib:jna",
        "//PresContest/lib:vlc",
    ],
    resources = glob([
        "data/**/*",
        "font/**/*",
        "images/**/*",
    ]),
    visibility = ["//visibility:public"],
)
