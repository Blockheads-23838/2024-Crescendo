package org.firstinspires.ftc.teamcode.auto;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.common.Constants;
import org.firstinspires.ftc.teamcode.drive.SampleMecanumDrive;
import org.firstinspires.ftc.teamcode.trajectorysequence.TrajectorySequence;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;

@Config
@com.qualcomm.robotcore.eventloop.opmode.Autonomous (name = "Red auto")
public class AutonomousRed extends LinearOpMode {

    private DcMotorEx lift;
    private DcMotorEx trident = null;
    private Servo leftTridentServo = null;
    private Servo rightTridentServo = null;
    private DcMotor intake = null;
    OpenCvCamera camera;
    PipelineRed pipeline = new PipelineRed();

    public static Pose2d toPropPose = new Pose2d(9, 36 - 6, Math.toRadians(180));
    public static double toPropPoseAngle = 225;
    public static Pose2d pixelDropPose = new Pose2d(12, 36 - 6, Math.toRadians(180));
    public static double pixelDropPoseAngle = 180;
    // private VisionPortal visionPortal;               // Used to manage the video source.
    // private AprilTagProcessor aprilTag;              // Used for managing the AprilTag detection process.

    @Override
    public void runOpMode() throws InterruptedException {
        // Camera activation
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        camera = OpenCvCameraFactory.getInstance().createWebcam(hardwareMap.get(WebcamName.class, "camera"), cameraMonitorViewId);

        camera.setPipeline(pipeline);
        camera.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
            @Override
            public void onOpened() {
                camera.startStreaming(1280, 720, OpenCvCameraRotation.UPRIGHT);
            }

            @Override
            public void onError(int errorCode) {

            }
        });

        // Scoring activation
        lift = hardwareMap.get(DcMotorEx.class, "lift");
        intake = hardwareMap.get(DcMotor.class, "intake");
        trident = hardwareMap.get(DcMotorEx.class, "trident");

        leftTridentServo = hardwareMap.get(Servo.class, "trident_left");
        rightTridentServo = hardwareMap.get(Servo.class, "trident_right");

        lift.setDirection(Constants.motorDirections.get("lift"));

        lift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        lift.setVelocity(1);
        leftTridentServo.setPosition(Constants.leftTridentClosedPosition);

        while (!isStarted() && !isStopRequested()) {
            telemetry.addData("Prop x value: ", pipeline.getJunctionPoint().x);
            telemetry.addData("Prop area: ", pipeline.getPropAreaAttr());
            telemetry.addLine("Place the purple pixel between the second and third compliant wheels from the left.");
            telemetry.addLine("It should be roughly centered.  It should be as close to touching the ground as possible WITHOUT touching the ground.");
            telemetry.addLine("Ensure the intake is at the bottom of its backlash-induced free-spinning zone so the pixel doesn't scrape the ground.");
            telemetry.update();
        }

        lift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        lift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        lift.setVelocity(1);

        // Drivetrain activation
        SampleMecanumDrive drive = new SampleMecanumDrive(hardwareMap);
        Pose2d startingPose = new Pose2d(15, -63, Math.toRadians(90));
        drive.setPoseEstimate(startingPose);


        TrajectorySequence rightTrajectory = drive.trajectorySequenceBuilder(startingPose)
                .splineToLinearHeading(new Pose2d(12, -33, Math.toRadians(0)), Math.toRadians(180))
                .UNSTABLE_addTemporalMarkerOffset(0, () -> {
                    intake.setPower(-1);
                })
                .UNSTABLE_addTemporalMarkerOffset(1, () -> {
                    intake.setPower(0);
                })
                .waitSeconds(1)
                .build();

        TrajectorySequence middleTrajectory = drive.trajectorySequenceBuilder(startingPose)
                .splineToLinearHeading(new Pose2d(15, -30, Math.toRadians(90)), Math.toRadians(90))
                .build();

        TrajectorySequence leftTrajectory = drive.trajectorySequenceBuilder(startingPose)
                .splineToLinearHeading(new Pose2d(2, -39, Math.toRadians(90)), Math.toRadians(210))
                .build();


        double propX = pipeline.getJunctionPoint().x;
        double propArea = pipeline.getPropAreaAttr();

        if (propArea < 10000) { // None detected, we assume left spike mark
            drive.followTrajectorySequence(leftTrajectory);
        } else if (propX > 500) { // right spike mark; different from blue because we start towards the right mark instead of the left
            drive.followTrajectorySequence(rightTrajectory);
        } else { // middle spike mark
            drive.followTrajectorySequence(middleTrajectory);
        }
        leftTridentServo.setPosition(Constants.leftTridentOpenPosition);
    }
}
