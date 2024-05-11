package org.zhiwei.compose.screen.layout_state

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.zhiwei.compose.ui.widget.Title_Desc_Text
import org.zhiwei.compose.ui.widget.Title_Sub_Text
import org.zhiwei.compose.ui.widget.Title_Text
import kotlin.random.Random

/**
 * 演示Effect相关的使用
 */
@Composable
internal fun Effect_Screen(modifier: Modifier = Modifier) {
    Column(modifier) {
        UI_RememberEffect()
        UI_rememberScopeUpdate()
    }
}


//region effect相关

/**
 * 演示与Compose相关的 效应 Effect 函数的使用
 * 简要理解，伴随composable的UI控件生命周期 ：创建---组合（单/多次）---销毁 ，可以使用一些Effect函数来感知其周期。
 * ⚠️：一般不建议在Composable函数中封装Effect，而应该根据业务需求，尽可能低限度的使用必要的Effect，通常compose函数要与状态分离，实现设计架构理念里的 单向数据流的要求。
 * 1. LaunchEffect，可用于感知当前compose函数的创建，它接收参数key，当key不变时，它伴随compose生命周期，仅回调一次；当key变化时，它会再次回调。（是在compose内部组合完成时，最后一个调用它）
 * 2. SideEffect，每次compose的组合都会调用。可理解为传统view的draw或show。也是在每次compose组合完成组合后调用。
 * 3. DisposableEffect，销毁效应，也是伴随生命周期，它自身接收key参数，类似于LaunchEffect，会随生命周期创建一次（如果key不变的话），其内部的onDispose则会在compose销毁时候调用。若key变化，也会回调（包括内部onDispose）。
 * 它们的回调时机，根其在compose函数中的代码顺序位置无关。
 * 4. remember可以接收多个key作为标记内部数据是否变化的flag，即使内部数据没更改，而key变化了，也会触发外部接收者的数据变化。
 */
@Composable
private fun UI_RememberEffect() {
    //这里remember加了key参数，如果key修改，则也会触发keyNum变化，即使内部数值可能没有修改。⚠️这里keyStr也必须是remember{mutableState}的，否则compose是感知不到变化的。
    var keyStr by remember { mutableStateOf("key") }
    //每次重组，会让remKeyNum归零，为初始值;⚠️注意使用mutableStateOf，需要变化对象，才会触发数据变更，如果只是更改TNum的内部值，是不会触发的。因为TNum内部值没有进入到compose作用域的监管。
//    val remKeyNum = remember { mutableStateOf(TNum(0)) }
    val remKeyNum = remember(keyStr) { mutableIntStateOf(0) }

    //compose组件每次绘制组合，都会调用的Effect
    SideEffect {
        println("♻️：。。。每次组合都调用。。。")
    }

    //如果key不变，则compose组合创建时候会调用，onDispose在compose销毁时候回调。可用于处理资源释放，管理lifeCycle等。如果key变化，则会触发回调。
    DisposableEffect(key1 = null) {
        println("🧵：>>> DisposableEffect关联 <<<")
        onDispose {
            println("🗑️：---销---毁---")
        }
    }

    //闯将compose的时候调用，如果key不变，则生命周期内仅调用一次。如果key变化，则会触发回调。
    LaunchedEffect(key1 = null) {
        println("🚀：->->->->->-> 启动 ---> --> ->")
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        println("⌚️：***进入column***")
        Title_Text(title = "Effect")
        Title_Sub_Text(title = "1. 演示remember标记key的变化来引起compose作用域的重组，以及使用LaunchEffect、SideEffect和DisposableEffect来感知composable的生命周期。")
        //点击此按钮，会让remKeyNum的remember的key变化，也就会触发它刷新数据，即使其内部数字未变更。
        Button(onClick = { keyStr = "keyStr${Random.nextInt(3)}" }) {
            println("🍎️：...变更key。。。")
            Text(text = "点击变更key")
        }
        //正常的变更数值
        Button(onClick = { remKeyNum.intValue++ }) {
            println("🪮：…………………………数字变更")
            Text(text = "点击计数：${remKeyNum.intValue}")
        }
        //感知变化。
        Text(text = "显示数字🔢：${remKeyNum.intValue}")
        println("🧮🔢：·······················")
    }
    //外部的重组的时候，会输出log
    println("🏃🏃🏃🏃🧗～～～～～～～～～～")
}

private data class TNum(var num: Int)

/**
 * 演示rememberCoroutineScope和rememberUpdatedState
 * 1. LaunchEffect启动一个伴随compose生命周期的协程，它会随LaunchEffect而创建和销毁；而rememberCoroutineScope创建的协程会跟随声明处的compose的生命周期。
 */
@Composable
private fun UI_rememberScopeUpdate() {
    //该协程生命周期会伴随UI_rememberScopeUpdate。LaunchEffect可能会因为key变化而重建。
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var counter by remember { mutableIntStateOf(0) }
    Title_Sub_Text(title = "2. 演示rememberCoroutineScope和rememberUpdatedState的使用")
    Title_Desc_Text(desc = "rememberCoroutineScope简单理解为创建一个协程，伴随调用处的compose的生命周期。而rememberUpdatedState则会及时响应外部数据的变更。")
    Column(
        Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(2.dp, Color.Cyan),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(onClick = {
            coroutineScope.launch {
                Toast.makeText(context, "🈵🦶你", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text(text = "点击弹toast")
        }
        OutlinedButton(onClick = { counter++ }) {
            Text(text = "点击增量$counter")
        }
        Text(text = "数字$counter")
        HorizontalDivider()
        //独立的compose函数，入参变化，来演示它内部的感知
        NumZone(input = counter)
    }
}

//注意看，不同的数据接收类型方式，入参变化时候，compose重组，但获取值是否更新，却不一样。
@Composable
private fun NumZone(input: Int) {
    val rememberUpdatedStateInput by rememberUpdatedState(input)
    val rememberedInput = remember { input }
    val rememberedStateInput = remember { mutableIntStateOf(input) }
    Text(text = "使用rememberUpdatedState：$rememberUpdatedStateInput")
    Text(text = "使用remember：$rememberedInput")
    Text(text = "使用remember加mutableStateOf：${rememberedStateInput.intValue}")
    Text(text = "原始数据：$input")
}

//endregion

//region 其他相关

@Composable
private fun UI_Other() {
    //produceState用于从composable创建state出去给其他地方使用，结果可给非compose的业务逻辑使用。
    //它必须在@composable的函数中
    val produceState = produceState(initialValue = TNum(0)) {
        //按照某种业务规则，产生新的状态的值
        value = TNum(2)
    }
    genTNumState()
    //snapshotFlow可用于创建一个flow数据流，可用在composable或非composable函数中
    val snapshotFlow = snapshotFlow {
        TNum(9)
    }

    //⚠️Modifier的多个操作符，可以包含可变数据，比如height可能是外部入参可变值，offSetX等。如果Modifier的某个数据感知变化，
    //那么在使用该Modifier的地方，都会触发重组。
    var height by remember { mutableIntStateOf(20) }
    val modifier = Modifier
        .fillMaxWidth()
        .height(height.dp)
    Box(modifier = modifier)
    //如上，如果某地方，修改了height的数值，那么modifier也会变化，使用modifier的compose组件，就会触发重组。
}

private fun genTNumState(): State<TNum> {
    //derivedStateOf用于在非Composable函数中，创建state值，可用于给compose使用,而不用使用remember；
    //它不能放在@composable的函数中。
    return derivedStateOf {
        TNum(2)
    }
}

//endregion

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun EffectPreview() {
    Effect_Screen()
}