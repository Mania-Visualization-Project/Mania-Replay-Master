#include <iostream>
#include <opencv2/opencv.hpp>
#include <random>
#include "me_replaymaster_ReplayMaster.h"

#define FPS 60
#define BLOCK_HEIGHT 40
#define GAME_WIDTH 540
#define GAME_HEIGHT 960
#define ACTION_HEIGHT 5
#define COLLUMN_PADDING_RATIO 0.1
#define TIME_INTERVAL (1000.0 / FPS)
#define TYPE_IMAGE CV_8UC3

using namespace cv;
using namespace std;

int pixelPerFrame;
double timeWindow;
int columnWidth;

struct Note {
    long timeStamp;
    int column;
    long duration;
    int judgementStart;
    int judgementEnd;

    inline long getEndTime() {
        return timeStamp + duration;
    }

    Note(long timeStamp, int column, int duration, int judgementStart, int judgementEnd)
            : timeStamp(timeStamp), column(column), duration(duration),
              judgementStart(judgementStart), judgementEnd(judgementEnd) {}
};

static void setup(jint key, jint speed) {
    columnWidth = (int) ((double) GAME_WIDTH / key);
    pixelPerFrame = speed;
    timeWindow = (double) GAME_HEIGHT / pixelPerFrame * TIME_INTERVAL;
}

inline static int timeToHeight(double t) {
    return (int) (((double) (t)) / timeWindow * GAME_HEIGHT);
}

static void readNote(istream &stream, vector<Note> &vector) {
    long timeStamp;
    int column;
    long duration;
    int judgementStart;
    int judgementEnd;
    stream >> timeStamp >> column >> duration >> judgementStart >> judgementEnd;
    vector.emplace_back(timeStamp, column, duration, judgementStart, judgementEnd);
}

static inline Scalar getJudgementColor(int judgement) {
    switch (judgement) {
        case 0:
            return Scalar(255, 255, 255);
        case 1:
            return Scalar(55, 210, 255);
        case 2:
            return Scalar(32, 208, 121);
        case 3:
            return Scalar(197, 104, 30);
        case 4:
            return Scalar(155, 52, 225);
        default:
            return Scalar(0, 0, 255);
    }
}


void renderNote(Mat &image, Note &note, double time, bool isBase, int judgement) {
    int x = (int) (note.column * columnWidth);
    int y = timeToHeight(time);
    int h = MAX(BLOCK_HEIGHT, timeToHeight(note.duration));
    int width = columnWidth;
    if (!isBase) {
        h = ACTION_HEIGHT;
        x += width / 5;
        width -= 2 * width / 5;
    }
    if (y - h < 0) h = y;
    Scalar color = getJudgementColor(judgement);
    x += columnWidth * COLLUMN_PADDING_RATIO;
    width -= 2 * columnWidth * COLLUMN_PADDING_RATIO;
    rectangle(image, Point(x, y), Point(x + width, y - h),
              color, isBase ? 3 : FILLED);
}

void renderActionLN(Mat &image, Note &note, double currentTime) {
    int width = ACTION_HEIGHT / 2;
    int x = (int) (note.column * columnWidth) + columnWidth / 2;
    int y = timeToHeight(currentTime - note.timeStamp);
    int h = timeToHeight(note.duration);
    if (y < h) h = y;
    Scalar color(50, 50, 50);
    for (int currentH = ACTION_HEIGHT * 2;
         currentH + ACTION_HEIGHT <= h; currentH += ACTION_HEIGHT * 2) {
        rectangle(image,
                  Point(x - width, y - currentH),
                  Point(x + width, y - currentH - ACTION_HEIGHT),
                  color, -1);
    }
}

int render(int beginIndex, vector<Note> &data, double time, Mat &image, bool isBase) {
    const int dataSize = data.size();
    for (int i = beginIndex; i < dataSize && data[i].timeStamp <= time; i++) {
        auto &note = data[i];
        if (timeToHeight(time - note.getEndTime()) > GAME_HEIGHT) {
            continue;
        }
        renderNote(image, note, time - note.timeStamp, isBase, note.judgementStart);

        if (!isBase && note.duration != 0) { // hold LN, render end
            renderNote(image, note, time - note.getEndTime(), false,
                       note.judgementEnd);
            renderActionLN(image, note, time);
        }
    }
    return beginIndex;
}

/*
 * Class:     me_replaymaster_ReplayMaster
 * Method:    nativeRender
 * Signature: (IILjava/lang/String;ILjava/lang/String;Ljava/lang/String;)
 *
 * external fun nativeRender(key: Int,
                              beatSize: Int, beatNotes: String,
                              replaySize: Int, replayNotes: String,
                              resultPath: String)
 */
JNIEXPORT void JNICALL Java_me_replaymaster_ReplayMaster_nativeRender
        (JNIEnv *env, jclass, jint key, jint beatSize, jstring jBeatNotes, jint replaySize,
         jstring jReplayNotes, jstring jPath, jint speed) {

    setup(key, speed);

    auto beatNotes = env->GetStringUTFChars(jBeatNotes, JNI_FALSE);
    auto replayNotes = env->GetStringUTFChars(jReplayNotes, JNI_FALSE);
    auto path = env->GetStringUTFChars(jPath, JNI_FALSE);

    Size imageSize(GAME_WIDTH, GAME_HEIGHT);
    VideoWriter writer(string(path), VideoWriter::fourcc('D', 'I', 'V', 'X'), FPS, imageSize);
    Mat image(imageSize, TYPE_IMAGE);
    Mat dark(imageSize, TYPE_IMAGE, Scalar::all(0));
    vector<Note> beats;
    vector<Note> replay;

    stringstream beatStream(beatNotes), replayStream(replayNotes);

    for (int i = 0; i < beatSize; i++) {
        readNote(beatStream, beats);
    }

    for (int i = 0; i < replaySize; i++) {
        readNote(replayStream, replay);
    }

    auto duration = beats[beatSize - 1].getEndTime() + 2000;
    cout << "duration: " << duration << "ms" << endl;
    int lastBeat = 0, lastReplay = 0;

    int progress = 0;

    for (double time = timeWindow; time < duration; time += TIME_INTERVAL) {
        dark.copyTo(image);
        if ((int) (time / duration * 100) > progress) {
            progress = (int) (time / duration * 100);
            if (progress % 5 == 0) {
                cout << progress << "%" << endl;
            }
        }

        lastBeat = render(lastBeat, beats, time, image, true);
        lastReplay = render(lastReplay, replay, time, image, false);

        writer.write(image);
    }

    image.release();
    writer.release();

    env->ReleaseStringUTFChars(jBeatNotes, beatNotes);
    env->ReleaseStringUTFChars(jReplayNotes, replayNotes);
    env->ReleaseStringUTFChars(jPath, path);
}

