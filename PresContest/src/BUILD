java_library(
    name = "presentations",
    srcs = glob(["**/*.java"]),
    deps = [
        "//ContestModel/src:contestModel",
        "//ContestModel/lib:svgSalamander",
        "//PresCore/src:presentCore",
        "//PresContest/lib:jna",
        "//PresContest/lib:vlcj",
    ],
    resources = glob([
        "data/**/*",
        "font/**/*",
        "images/**/*",
    ]),
    visibility = ["//visibility:public"],
)
