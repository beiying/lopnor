//
// Created by beiying on 2020/10/23.
//

#ifndef LOPNOR_CASCADEDETECTORADAPTER_H
#define LOPNOR_CASCADEDETECTORADAPTER_H

#include "opencv2/opencv.hpp"

using namespace cv;

class CascadeDetectorAdapter: public DetectionBasedTracker::IDetector {
public:
    CascadeDetectorAdapter(Ptr<CascadeClassifier> detector): IDetector(), Detector(detector) {
        CV_Assert(detector);
    }

    void detect(const Mat &image, std::vector<Rect> &objects) {
        Detector->detectMultiScale(image, objects, scaleFactor, minNeighbours, 0, minObjSize);
    }
    virtual ~CascadeDetectorAdapter() {

    }
private:
    CascadeDetectorAdapter();
    Ptr<CascadeClassifier> Detector;
};


#endif //LOPNOR_CASCADEDETECTORADAPTER_H
