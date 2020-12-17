java_binary(
    name = "coachview",
    srcs = glob(["**/*.java"]),
    deps = [
	"//CoachView/lib:jna",
	"//CoachView/lib:vlcj",
        "//ContestModel/src:contestModel",
        "//PresCore/src:presentCore",
    ],
    main_class = "org.icpc.tools.coachview.CoachView",
    resources = glob([
        "font/**/*",
        "images/**/*",
    ]),
    visibility = ["//visibility:public"],
)
