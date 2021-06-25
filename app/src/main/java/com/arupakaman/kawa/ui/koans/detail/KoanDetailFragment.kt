package com.arupakaman.kawa.ui.koans.detail

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.arupakaman.kawa.KoansGenerator
import com.arupakaman.kawa.R
import com.arupakaman.kawa.data.database.entities.Koan
import com.arupakaman.kawa.data.pref.MyAppPref
import com.arupakaman.kawa.databinding.FragmentKoanDetailBinding
import com.arupakaman.kawa.ui.koans.KoansActivitySharedViewModel
import com.arupakaman.kawa.ui.koans.detail.adapter.KoanDetailAdapter
import com.arupakaman.kawa.utils.*
import com.arupakaman.kawa.utils.motions.setupSharedElementTransitionToContainerTransform
import com.flavours.AdManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KoanDetailFragment : Fragment() {

    private var binding:FragmentKoanDetailBinding?=null

    private val koansActivitySharedViewModel by lazy { ViewModelProvider(requireActivity()).get(KoansActivitySharedViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSharedElementTransitionToContainerTransform()

        if (arguments?.containsKey("index")==true)
            setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        val fragmentKoanDetailBinding = FragmentKoanDetailBinding.inflate(layoutInflater)
        binding=fragmentKoanDetailBinding
        return fragmentKoanDetailBinding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.lifecycleOwner = viewLifecycleOwner
        binding?.koansActivitySharedViewModel = koansActivitySharedViewModel

        if (arguments?.containsKey("index")==false)
        {
            koansActivitySharedViewModel.setAllKoansForDetail()
        }
        /*else{
            showRightSwipeGesture()
        }*/

        setObserver()


        binding?.viewPager2?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                koansActivitySharedViewModel.liveKoanListForDetail.value?.let {
                    val any = it[position]
                    if (any is Koan)
                        koansActivitySharedViewModel.setCurrentKoan(any)
                }
            }
        })

        KoansGenerator(requireContext()).prepareJsonArray()

        //prepareJsonArray()

        binding?.partialAdContainer?.let {container->
            AdManager.showAd(container,AdManager.BANNER_AD_KOAN_DETAIL)
        }

        showLeftSwipeGesture()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_koan_detail,menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId==R.id.itemOpenDrawer)
            koansActivitySharedViewModel.setOpenDrawerEvent()
        return super.onOptionsItemSelected(item)
    }

    /*private fun showRightSwipeGesture(){
        if (!MyAppPref.isKoanDetailShownFromListing)
        {
            lifecycleScope.launch(Dispatchers.Default){
                delay(1000)
                withContext(Dispatchers.Main){
                    activity?.showGestureIntro(TYPE_RIGHT_SWIPE){
                        koansActivitySharedViewModel.setOpenDrawerEvent()
                    }
                    MyAppPref.isKoanDetailShownFromListing=true
                }
            }
        }
    }*/

    private fun showLeftSwipeGesture(){
        if (!MyAppPref.isKoanDetailShown)
        {
            lifecycleScope.launch(Dispatchers.Default){
                delay(1000)
                withContext(Dispatchers.Main){
                    activity?.showGestureIntro(TYPE_LEFT_SWIPE){
                        binding?.viewPager2?.let {pager->
                            pager.setCurrentItem(pager.currentItem+1,true)
                        }
                    }
                    MyAppPref.isKoanDetailShown=true
                }
            }
        }
    }


    private fun getValidIndexOfLastVisitedKoan(): Int {
        koansActivitySharedViewModel.liveKoanListForDetail.value?.let {list->
            val model = list.singleOrNull { if(it is Koan) it.id==MyAppPref.currentKoanId else false }
            if (model!=null)
            {
                val index = list.indexOf(model)
                if (index!=-1)
                {
                    return index
                }
            }
        }
        return 0
    }

    private fun setCurrentItem(index:Int){

        binding?.viewPager2?.post {
            binding?.viewPager2?.setCurrentItem(index,false)
            Log.d("detail","setCurrentItemCalled")
        }
    }

    private fun setObserver(){
        koansActivitySharedViewModel.liveKoanListForDetail.observe(viewLifecycleOwner,{
            Log.d("koanList:", "from detail"+it.size.toString())

            binding?.viewPager2?.bindKoanDetailAdapter(it)

            try {
                val bundle = KoanDetailFragmentArgs.fromBundle(requireArguments())
                val position = bundle.index
                setCurrentItem(position)
            }catch (e:Exception){
                Log.d("onViewCreated",e.toString())
                setCurrentItem(getValidIndexOfLastVisitedKoan())
            }
        })

        koansActivitySharedViewModel.liveKoanTextSize.observe(viewLifecycleOwner, {
            context?.getDimenFromSize(it)?.let {textSizeInDimen->
                updateKoanTextStyleInAdapter {adapter->
                    adapter.textSizeInDimen = textSizeInDimen
                }
            }
        })

        koansActivitySharedViewModel.liveKoanTypeFace.observe(viewLifecycleOwner, {typeface->

            updateKoanTextStyleInAdapter {adapter->
                adapter.typeface = typeface
            }
        })
    }

    private fun updateKoanTextStyleInAdapter(block:(it:KoanDetailAdapter)->Unit){
        val viewPager2 = binding?.viewPager2 ?: return
        val adapter = viewPager2.adapter
        if (adapter is KoanDetailAdapter)
        {
            block(adapter)

            // notify the current item
            adapter.notifyItemChanged(viewPager2.currentItem)

            //need to notify previous and next item also because viewpager2 already loaded left and right item
            for (i in 1..2){
                val previousItem = viewPager2.currentItem - i
                if (previousItem>=0)
                    adapter.notifyItemChanged(previousItem)
            }

            val nextItem = viewPager2.currentItem + 1
            if (nextItem<adapter.itemCount)
                adapter.notifyItemChanged(nextItem)
        }
    }

    override fun onDestroyView() {
        AdManager.destroyAd(AdManager.BANNER_AD_KOAN_DETAIL)
        binding=null
        super.onDestroyView()
    }
}