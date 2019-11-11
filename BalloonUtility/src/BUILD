java_binary(
    name = "balloonutil",
    srcs = glob(["**/*.java"]),
    deps = [
        "//ContestModel/src:contestModel",
        "//PresCore/src:presentCore",
        "//SWTLauncher/lib:swt",
        "//SWTLauncher/src:swtLauncher",
    ],
    main_class = "org.icpc.tools.balloon.BalloonUtility",
    resources = glob([
        "images/**/*",
    ]),
    visibility = ["//visibility:public"],
)
