package org.zhiwei.jetpack.components.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.zhiwei.jetpack.components.R

/**
 * 这部分主要演示除dataBinding外的，主要的几个jetpack组件的用法；
 * 1. liveData、lifecycle、viewModel，room，paging，work，navigation
 * 2. 其实在JetpackActivity中就已经使用了navigation结合bottomNavigationView的简单用法
 */
class JetpackFragment : Fragment() {

    //fragment-ktx提供的扩展函数，便于获取viewModel的实例；
    // 该方式获取的vm，不同的fragment对象获取后的vm，不是同一个实例。所以vm中liveData发送数据，activity中观察不到，如果用下面的vm，就可以。
    private val vm: JetpackViewModel by viewModels()

    //fragment-ktx提供的扩展函数，便于获取该fragment依附的activity的viewModel的实例。
    // 该方式获取的vm，如果是同一个activity下不同的fragment获取vm，对象是同一个。
//    private val vm: JetpackViewModel by activityViewModels()

    //todo ⚠️：这么些使用navigation的时候，会有个场景bug；从当前页面navigate到其他fragment页面在返回的时候，这里会调用onCreateView/onViewCreated，但是不会重新onCreate。于切换bottomNavigation不同。
    //如此，则这些view的定义，就失效了。事件什么的都无用了。,所以使用navigation时候，view不要用这种lazy方式，而是在onCreateView/onViewCreated来设置。
//    private val tvLive: TextView by lazy { requireView().findViewById(R.id.tv_live_ret_jetpack) }
//    private val tvSwitchLive: TextView by lazy { requireView().findViewById(R.id.tv_live_switch_ret_jetpack) }
//    private val btnWork: Button by lazy { requireView().findViewById(R.id.btn_work_jetpack) }
    private lateinit var tvLive: TextView
    private lateinit var tvSwitchLive: TextView
    private lateinit var btnWork: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.w(TAG, "----- ---- onCreate: 创建Fragment ,可以看得出，每次切换tab，都是新建的fragment")
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        Log.d(TAG, "onCreateView: 创建FragmentView")
        //这里注意，第三个参数一定要设置false，否则报错
        return inflater.inflate(R.layout.fragment_jetpack, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: 渲染View层")
        //如此写法，就可以保证view对象都是每次UI新建的。
        tvLive = view.findViewById(R.id.tv_live_ret_jetpack)
        tvSwitchLive = view.findViewById(R.id.tv_live_switch_ret_jetpack)
        btnWork = view.findViewById(R.id.btn_work_jetpack)
        testLiveData()
        testWork()
    }

    //region liveData
    private val TAG = "JetpackFragment"
    private fun testLiveData() {
        //模拟生成数据，配合上面vm的获取方式，如果vm是fragment的，就取消注释，如果vm是activity的，就注释下面一行，用activity中的这行代码；
        //为的是便于演示效果
        vm.startSendScore()
        //观察live的数据变化，不要关联生命周期的lifecycleOwner，observeForever不需要。
        //viewLifecycleOwner是fragment的，activity就是自身。
        vm.liveScore.observe(viewLifecycleOwner) { str ->
            //Fragment观察👀数据
//            Log.d(TAG, "Fragment观察👀数据:$str")
            tvLive.text = "文本$str"
        }
        vm.switchMapLive().observe(viewLifecycleOwner) {
//            Log.i(TAG, "Switch Map观察👀数据:$it")
            tvSwitchLive.text = "数字$it"
        }

        testDistinct()
        testMediator()

    }

    private fun testDistinct() {
        liveData<Int> {
            emit(1)
            delay(200)
            emit(2)
            delay(200)
            emit(2)
            delay(200)
            emit(3)
            delay(200)
            emit(2)
            //记住这个distinctUntilChanged仅判断两次相邻的谁是否变化，而不管之前或之后会否再有一样的。
        }.distinctUntilChanged().observe(viewLifecycleOwner) {
            //重复的2，只会一次。
            //            Log.w(TAG, "Distinct 观察👀数据:$it")
        }
    }

    private fun testMediator() {
        //中介liveData,可用作桥接多个数据源，同一合并发送
        val liveOne = MutableLiveData<Int>()
        val liveTwo = MutableLiveData<String>()
        val merge = MediatorLiveData<Any>()
        merge.addSource(liveOne) {
            merge.postValue(it)
            //根据需要在适当的时候，移除数据源
            if (it == 4) merge.removeSource(liveOne)
        }
        merge.addSource(liveTwo) {
            merge.postValue(it)
        }

        merge.observe(viewLifecycleOwner) {
            Log.d(TAG, "MediatorLiveData 观察👀数据:$it")
        }
        lifecycleScope.launch {
            repeat(10) {
                delay(200)
                liveOne.postValue(it)
            }
        }
        lifecycleScope.launch {
            repeat(10) {
                delay(500)
                liveTwo.postValue("2️⃣ $it")
            }
        }
    }
    //endregion

    private fun testWork() {
        btnWork.setOnClickListener {
            //navigation跳转指定页面的fragment,这种方式参数需要arguments节点定义
//            findNavController().navigate(
//                R.id.work_fragment,
//                bundleOf("taskName" to "JtKt任务", "taskTime" to 200)
//            )
            //另一种方式，使用route的方式跳转到航，不过此时传参数，就要求graph中fragment节点定义route，且包含参数名
            //格式是 route_path_name/{paramOne}/{paramTwo}
            findNavController().navigate(
                "route_nav_work_jetpack/你好/300",
                navOptions = navOptions { launchSingleTop = true },
                navigatorExtras = FragmentNavigatorExtras()
            )
        }
    }

}