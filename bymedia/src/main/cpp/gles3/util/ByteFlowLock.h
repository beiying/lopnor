//
// Created by dajia on 2022/1/6.
//

#ifndef LOPNOR_BYTEFLOWLOCK_H
#define LOPNOR_BYTEFLOWLOCK_H
#include <pthread.h>

class BYNativeSyncLock {
public:
    BYNativeSyncLock() {
        pthread_mutexattr_init(&m_attr);
        pthread_mutexattr_settype(&m_attr, PTHREAD_MUTEX_RECURSIVE);
    }

private:
    pthread_mutex_t m_mutex;
    pthread_mutexattr_t m_attr;
};

#endif //LOPNOR_BYTEFLOWLOCK_H
