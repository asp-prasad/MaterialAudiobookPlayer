package voice.app.features.bookPlaying.selectchapter

import android.app.Dialog
import android.os.Bundle
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.coroutines.launch
import voice.app.R
import voice.app.databinding.SelectChapterRowBinding
import voice.app.injection.appComponent
import voice.app.misc.groupie.BindingItem
import voice.common.BookId
import voice.common.conductor.DialogController
import voice.data.ChapterMark
import voice.data.getBookId
import voice.data.putBookId
import javax.inject.Inject

private const val NI_BOOK_ID = "ni#bookId"

class SelectChapterDialog(bundle: Bundle) : DialogController(bundle) {

  @Inject
  lateinit var viewModel: SelectChapterViewModel

  constructor(bookId: BookId) : this(
    Bundle().apply {
      putBookId(NI_BOOK_ID, bookId)
    },
  )

  init {
    appComponent.inject(this)
    viewModel.bookId = args.getBookId(NI_BOOK_ID)!!
  }

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    val viewState = viewModel.viewState()
    val items = viewState.chapters.mapIndexed { index, mark ->
      val listener = View.OnClickListener {
        viewModel.chapterClicked(index)
      }
      BindingItem<SelectChapterRowBinding, ChapterMark>(
        mark,
        R.layout.select_chapter_row,
        SelectChapterRowBinding::bind,
      ) { data, position ->
        root.setOnClickListener(listener)
        @Suppress("SetTextI18n")
        textView.text = "${position + 1} - ${data.name}"
        textView.setCompoundDrawablesWithIntrinsicBounds(
          0,
          0,
          if (position == viewState.selectedIndex) R.drawable.ic_equalizer else 0,
          0,
        )
      }
    }

    val adapter = GroupAdapter<GroupieViewHolder>()
    adapter.addAll(items)
    return MaterialDialog(activity!!).apply {
      customListAdapter(adapter)
      if (viewState.selectedIndex != null) {
        getRecyclerView().layoutManager!!.scrollToPosition(viewState.selectedIndex)
      }
    }
  }

  override fun onAttach(view: View) {
    super.onAttach(view)
    lifecycleScope.launch {
      viewModel.viewEffects.collect {
        when (it) {
          SelectChapterViewEffect.CloseScreen -> {
            dismissDialog()
          }
        }
      }
    }
  }
}
