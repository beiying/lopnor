//
// Created by dajia on 2020/9/8.
//

#include <queue>
#include <pthread.h>

using namespace std;

template <typename T>
class SafeQueue {
public:
    SafeQueue() {
        pthread_mutex_init(&mutex, NULL);
        pthread_cond_init(&cond, NULL);
    }

    ~SafeQueue() {
        pthread_cond_destroy(&cond);
        pthread_mutex_destroy(&mutex);
    }

    void put(T new_value) {
        //锁，和智能指针原理类似，自动释放
        pthread_mutex_lock(&mutex);
        if(work) {
            q.push(new_value);
            pthread_cond_signal(&cond);
        }else {

        }
        pthread_mutex_unlock(&mutex);
    }

    int get(T& value) {
        int ret = 0;
        pthread_mutex_lock(&mutex);

        //在多核处理器下，由于竞争可能有虚假唤醒
        while(work && q.empty()) {
            pthread_cond_wait(&cond, &mutex);
        }
        if (!q.empty()) {
            value = q.front();
            q.pop();
            ret = 1;
        }
        pthread_mutex_unlock(&mutex);
        return ret;
    }

    void setWork(int work) {
        pthread_mutex_lock(&mutex);
        this->work = work;
        pthread_cond_signal(&cond);
        pthread_mutex_unlock(&mutex);
    }

    int empty() {
        return q.empty();
    }

    int size() {
        return q.size();
    }

    void clear() {
        pthread_mutex_lock(&mutex);
        while(!q.empty()) {
            q.pop();
        }
        pthread_cond_signal(&cond);
        pthread_mutex_unlock(&mutex);
    }

private:
    queue<int> q;
    int work;
    pthread_mutex_t mutex;
    pthread_cond_t cond;
};


